package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
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
     * 根绝订单号查询订单
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
    public OrderVO getOrderDetailByOrderId(Long id) {
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


}
