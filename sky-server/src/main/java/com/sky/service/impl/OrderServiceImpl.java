package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.HttpClientUtil;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Autor：林建威
 * @DateTime：2024/4/30 16:37
 **/

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WebSocketServer webSocketServer;

    @Value("${sky.shop.address}")
    private String shopAddress;

    @Value("${sky.baidu.ak}")
    private String ak;

    /**
     * 用户下单
     *
     * @param ordersSubmitDTO
     * @return
     */
    @Transactional //要么全成功,要么全失败
    @Override
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        //1.处理各种业务异常（地址簿为空、购物车数据为空）
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            //抛出业务异常
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        //校验地址是否超出配送范围
        checkOutOfRange(addressBook.getCityName() + addressBook.getDistrictName() + addressBook.getDetail());

        //查询当前登录用户id得购物车商品信息是否为空
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);
        if (shoppingCartList == null || shoppingCartList.size() == 0) {
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        //2.向订单表插入一条数据
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setOrderTime(LocalDateTime.now());
        orders.setUserId(userId);
        orders.setConsignee(addressBook.getConsignee());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setPhone(addressBook.getPhone());
        orders.setAddress(addressBook.getDetail());
        orderMapper.insert(orders);

        //定义一个集合存储订单明细后批量插入
        List<OrderDetail> orderDetails = new ArrayList<>();
        for (ShoppingCart cart : shoppingCartList) {
            //cart依次表示每一个购物车元素
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(orders.getId()); //设置当前订单详细表得订单id
            orderDetails.add(orderDetail);
        }
        //3.批量插入数据到订单详细表
        orderDetailMapper.insertBatch(orderDetails);
        //4.清空当前用户购物车得数据
        shoppingCartMapper.deleteByUserId(userId);

        //5.封装OrderSubmitVO返回结果
        OrderSubmitVO orderSubmitVO = new OrderSubmitVO();
        orderSubmitVO.setId(orders.getId());
        orderSubmitVO.setOrderTime(orders.getOrderTime());
        orderSubmitVO.setOrderNumber(orders.getNumber());
        orderSubmitVO.setOrderAmount(orders.getAmount());
        return orderSubmitVO;
    }

    /**
     * 检查客户的收获地址是否超出配送范围
     * @param address
     */
    private void checkOutOfRange(String address) {
        Map map = new HashMap();
        map.put("address", shopAddress);
        map.put("output", "json");
        map.put("ak", ak);

        //获取店铺的经纬度坐标
        String shopCoordinate = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", map);

        //将改店铺这个字符串转换成json格式
        JSONObject jsonObject = JSON.parseObject(shopCoordinate);
        //判断从json格式中键名为status的值是否为0
        if (!jsonObject.getString("status").equals("0")) {
            //如果是0,则抛出异常
            throw new OrderBusinessException("店铺地址解析失败!");
        }

        //数据解析,第一个先获取一个键名为result的子对象,返回一个新的json对象,
        //再从result这个子对象中获取键名为location的子对象,返回一个新的json对象
        /*JSONObject result = jsonObject.getJSONObject("result");
        JSONObject location = result.getJSONObject("location");*/
        JSONObject location = jsonObject.getJSONObject("result").getJSONObject("location");

        //获取键名为lat的字符串内容
        String lat = location.getString("lat");
        String lng = location.getString("lng");
        //店铺的经纬度坐标
        String shopLatLng = lat + "," + lng;

        //获取用户收货地址的经纬度坐标
        map.put("address", address);
        String userCoordinate = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", map);
        //转成json格式
        jsonObject = JSON.parseObject(userCoordinate);
        if (!jsonObject.getString("status").equals("0")) {
            //如果不是0,则抛出异常
            throw new OrderBusinessException("用户地址解析失败!");
        }
        //数据解析
        location = jsonObject.getJSONObject("result").getJSONObject("location");
        lat = location.getString("lat");
        lng = location.getString("lng");
        //用户收货地址经纬度坐标
        String userLngLat = lat + "," + lng;

        map.put("origin", shopLatLng);
        map.put("destination", userLngLat);
        map.put("steps_info", "0");

        //路线规划
        String json = HttpClientUtil.doGet("https://api.map.baidu.com/directionlite/v1/driving", map);

        //转换json格式
        jsonObject = JSON.parseObject(json);
        if(!jsonObject.getString("status").equals("0")){
            //如果不是0,则抛出异常
            throw new OrderBusinessException("配送路线规划失败");
        }
        //数据解析
        JSONObject result = jsonObject.getJSONObject("result");
        //可能存在多条数据,使用强转为json数组类型
        JSONArray jsonArray = (JSONArray) result.get("routes");
        //获取json数组中索引为0的第一条数据
        JSONObject firstData = (JSONObject) jsonArray.get(0);
        //再从第一条数据中,获取键名为"distance"的值
        Integer distance = (Integer) firstData.get("distance");

        if (distance > 5000000) {
            throw new OrderBusinessException("超出配送范围!");
        }
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    @Override
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 获取当前登录用户id
        // Long userId = BaseContext.getCurrentId();
        // User user = userMapper.getById(userId);

        //调用微信支付接口，生成预支付交易单 ==========>跳过微信支付,因为无营业牌照
        /*JSONObject jsonObject = weChatPayUtil.pay(
                ordersPaymentDTO.getOrderNumber(), //商户订单号
                new BigDecimal(0.01), //支付金额，单位 元
                "苍穹外卖订单", //商品描述
                user.getOpenid() //微信用户的openid
        );

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }*/
        // JSONObject jsonObject = new JSONObject();
        //将json对象转换成类型为OrderPaymentVO的java对象"vo"
        // OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        //将从json对象中获取的统一下单接口设置为"vo"的统一下单接口返回的 prepay_id 参数值
        // vo.setPackageStr(jsonObject.getString("package"));
        // return vo;

        //获取订单号作为参数调用paySuccess方法,模拟微信支付已完成
        paySuccess(ordersPaymentDTO.getOrderNumber());
        //返回值直接返回空值
        return null;
    }

    /**
     * 根据订单号查询订单
     *
     * @param outTradeNo
     */
    @Override
    public void paySuccess(String outTradeNo) {
        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);

        //通过Websocket向客户端浏览器推送消息 type orderId content
        Map map = new HashMap();
        map.put("type", 1); //1 表示来单提醒 2表示催单提醒
        map.put("orderId", ordersDB.getId());
        map.put("content", "订单号" + outTradeNo);

        //转换成json字符串
        String json = JSON.toJSONString(map);
        //将结果推送到页面
        webSocketServer.sendToAllClient(json);
    }

    /**
     * 用户端订单分页查询
     *
     * @param pageNum
     * @param pageSize
     * @param status   订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
     * @return
     */
    @Override
    public PageResult pageQuery4User(int pageNum, int pageSize, Integer status) {
        //设置分页
        PageHelper.startPage(pageNum, pageSize);
        //创建一个分页查询数据传输对象
        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        //获取当前登录用户id设置为当前分页查询的用户
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        //设置状态为当前前端传递进来的状态参数
        ordersPageQueryDTO.setStatus(status);
        //设置分页条件
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);
        //创建一个泛型是订单视图对象的集合
        List<OrderVO> list = new ArrayList<>();
        //判断分页查询的结果数据是否为空
        if (page != null && page.getTotal() > 0) {
            for (Orders order : page) {
                System.out.println(order);
                //orders依次表示分页查询结果中的每一条数据，获取订单id
                Long orderId = order.getId();
                //根据订单id查询订单详情，因为可能返回多条数据，所以使用集合
                List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(orderId);
                //新建一个订单视图对象
                OrderVO orderVO = new OrderVO();
                //数据拷贝
                BeanUtils.copyProperties(order, orderVO);
                //设置订单详情
                orderVO.setOrderDetailList(orderDetails);
                //将结果插入到集合中
                list.add(orderVO);
            }
        }
        return new PageResult(page.getTotal(), list);
    }

    /**
     * 根据订单id获取订单详情
     *
     * @param id
     * @return
     */
    @Override
    public OrderVO details(Long id) {
        //根据id查询订单信息
        Orders orders = orderMapper.getByOrderId(id);
        //根据订单id查询订单详情
        List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(id);
        //将查询到的结果封装成ordervo
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(orderDetails);
        //返回封装结果
        return orderVO;
    }

    /**
     * 取消订单
     *
     * @param id
     */
    @Override
    public void cancel(Long id) {
        //首先根据订单id获取订单状态
        Orders orderDB = orderMapper.getByOrderId(id);
        //校验订单是否存在
        if (orderDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        //订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
        Integer status = orderDB.getStatus();
        //判断订单状态是否为待付款或则待接单，如是则直接取消，如果是待接单状态下还得进行退款
        if (status > 2) {
            //如果订单大于2,则抛出错误
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        //创建一个order对象
        Orders orders = new Orders();
        orders.setId(orderDB.getId());
        if (orderDB.equals(Orders.TO_BE_CONFIRMED)) {
            orders.setPayStatus(Orders.REFUND);
        }
        //取消订单后则需要修改状态为已取消
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason("用户取消订单!");
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);
    }

    /**
     * 当前订单再来一单
     *
     * @param id
     */
    @Transactional
    @Override
    public void getByOrderId(Long id) {
        //获取当前用户id
        Long userId = BaseContext.getCurrentId();
        //根据订单id查询订单详情
        List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(id);
        //使用流将订单详情顶对象转换成购物车对象,map对stream流中的元素操作,并将操作结果映射为新的元素
        List<ShoppingCart> shoppingCartList = orderDetails.stream().map(orderDetail -> {
                    ShoppingCart shoppingCart = new ShoppingCart();
                    //数据拷贝,但是忽略"id"的属性不进行拷贝
                    BeanUtils.copyProperties(orderDetail, shoppingCart, "id");
                    //设置当前用户id
                    shoppingCart.setUserId(userId);
                    shoppingCart.setCreateTime(LocalDateTime.now());
                    return shoppingCart;
                }).collect(Collectors.toList());//终止stream流,并通过Collectors.toList()将stream流中的元素收集到一个List集合中

        //将购物车对象批量插入到数据库中
        shoppingCartMapper.insertBatch(shoppingCartList);
    }

    /**
     * 管理端订单管理-分页查询
     * @param ordersPageQueryDTO
     * @return
     */
    @Override
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);

        //部分订单状态可能还要返回订单菜品,所以需要将orders转换成orderVO
        List<OrderVO> orderVOList = changeOrdersToOrderVO(page);

        return new PageResult(page.getTotal(), orderVOList);

    }

    /**
     * 各个状态的订单数量统计
     * 订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
     * @return
     */
    @Override
    public OrderStatisticsVO statistics() {
        //获取各个状态统计订单数量
        Integer confirmed = orderMapper.CountByStatus(Orders.CONFIRMED);
        Integer deliveryInProgress = orderMapper.CountByStatus(Orders.DELIVERY_IN_PROGRESS);
        Integer toBeConfirmed = orderMapper.CountByStatus(Orders.TO_BE_CONFIRMED);

        //新建一个订单统计视图对象
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        //将上面获取的数据进行属性设置
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);

        //最后将结果返回
        return orderStatisticsVO;
    }

    /**
     * 将分页查询的结果Orders转换成orderVO的视图对象
     * @param page
     * @return
     */
    private List<OrderVO> changeOrdersToOrderVO(Page<Orders> page) {
        //创建一个用来存储需要返回菜品信息的订单状态的集合
        List<OrderVO> orderVOList = new ArrayList<>();
        //用集合存储分页查询的结果
        List<Orders> ordersList = page.getResult();
        //遍历分页查询的结果集合,进行判断是否为空
        if (!CollectionUtils.isEmpty(ordersList)) {
            for (Orders orders : ordersList) {
                //如果非空,那么就将共同字段复制到orderVO中
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                //调用获取菜品字符串的方法,获取菜品信息
                String orderDishes = getOrderDishesStr(orders);
                //然后将结果封装返回
                orderVO.setOrderDishes(orderDishes);
                orderVOList.add(orderVO);
            }
        }
        return orderVOList;
    }

    /**
     * 根据订单id获取菜品信息字符串
     * @param orders
     * @return
     */
    private String getOrderDishesStr(Orders orders) {
        //查询订单菜品的详细信息(订单中的菜品和数量)
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());
        //将每一条菜品信息拼接成字符串(格式:菜品*3)
        List<String> orderDishList = orderDetailList.stream().map(orderDetail-> {
                String orderDish = orderDetail.getName() + "*" + orderDetail.getNumber() + ";";
                return orderDish;
        }).collect(Collectors.toList());
        //将订单对应的所有菜品信息拼接一起然后返回
        return String.join("", orderDishList);
    }

    /**
     * 接单
     * 订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
     * @param ordersConfirmDTO
     */
    @Override
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        //根据订单id获取订单信息
        Orders ordersDB = orderMapper.getByOrderId(ordersConfirmDTO.getId());
        //创建一个订单实体对象
        Orders orders = new Orders();
        //对相同属性字段进行数据拷贝
        BeanUtils.copyProperties(ordersDB, orders);
        //修改订单状态
        orders.setStatus(Orders.CONFIRMED);
        //保存修改后的订单状态
        orderMapper.update(orders);
    }

    /**
     * 拒单
     * @param ordersRejectionDTO
     */
    @Override
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) {
        //根据订单id获取订单信息
        Orders ordersDB = orderMapper.getByOrderId(ordersRejectionDTO.getId());
        //判断订单是否为空,或者订单状态是否处于"待接单"状态
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            //订单为空,或则订单不处于待接单状态则抛出异常
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        //因为拒单后,要进行退款,此时需要判断支付状态
        Integer payStatus = ordersDB.getPayStatus();
        if (payStatus == Orders.PAID) {
            //支付状态为已支付,则进行退款操作
            orders.setPayStatus(Orders.REFUND);
        }
        orders.setId(ordersDB.getId());
        orders.setStatus(Orders.CANCELLED);
        orders.setRejectionReason(ordersRejectionDTO.getRejectionReason());
        orders.setCancelTime(LocalDateTime.now());
        orders.setPayStatus(Orders.REFUND);
        orderMapper.update(orders);
    }

    /**
     * 管理端取消订单
     * @param ordersCancelDTO
     */
    @Override
    public void adminCancel(OrdersCancelDTO ordersCancelDTO) {
        Orders ordersDB = orderMapper.getByOrderId(ordersCancelDTO.getId());
        //创建一个订单实体类对象
        Orders orders = new Orders();
        //获取订单当前状态信息
        Integer payStatus = ordersDB.getPayStatus();
        //判断当前状态是否为已支付
        if (payStatus == Orders.PAID) {
            //如果当前处于已支付状态,取消则需要进行退款
            orders.setPayStatus(Orders.REFUND);
        }
        orders.setId(ordersDB.getId());
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelTime(LocalDateTime.now());
        orders.setCancelReason(ordersCancelDTO.getCancelReason());
        //将结果保存
        orderMapper.update(orders);
    }

    /**
     * 派送订单
     * @param id
     */
    @Override
    public void delivery(Long id) {
        //根据订单id获取订单信息
        Orders ordersDB = orderMapper.getByOrderId(id);
        //校验订单是否为空,或订单状态为已接单
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        orders.setStatus(Orders.DELIVERY_IN_PROGRESS);
        orderMapper.update(orders);

    }

    /**
     * 完成订单
     * @param id
     */
    @Override
    public void complete(Long id) {
        Orders ordersDB = orderMapper.getByOrderId(id);
        //校验订单
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        orders.setStatus(Orders.COMPLETED);
        orders.setDeliveryTime(LocalDateTime.now());
        orderMapper.update(orders);
    }

    /**
     * 用户催单
     * @param id
     */
    @Override
    public void reminder(Long id) {
        //根据订单id获取订单信息
        Orders ordersDB = orderMapper.getByOrderId(id);
        //校验订单
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        //通过Websocket向客户端浏览器推送消息 type orderId content
        Map map = new HashMap();
        map.put("type", 2); //1 表示来单提醒 2表示催单提醒
        map.put("orderId", id);
        map.put("content", "订单号" + ordersDB.getNumber());

        //转换成json字符串
        String json = JSON.toJSONString(map);
        //将结果推送到页面
        webSocketServer.sendToAllClient(json);
    }

}
