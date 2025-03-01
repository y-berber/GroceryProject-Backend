package com.example.grocery.business.concretes;

import com.example.grocery.business.abstracts.CustomerService;
import com.example.grocery.business.abstracts.OrderService;
import com.example.grocery.business.abstracts.PaymentService;
import com.example.grocery.business.abstracts.ProductService;
import com.example.grocery.business.constants.Messages.*;
import com.example.grocery.business.constants.Messages.LogMessages.LogInfoMessages;
import com.example.grocery.business.constants.Messages.LogMessages.LogWarnMessages;
import com.example.grocery.core.utilities.business.BusinessRules;
import com.example.grocery.core.utilities.exceptions.BusinessException;
import com.example.grocery.core.utilities.mapper.MapperService;
import com.example.grocery.core.utilities.results.DataResult;
import com.example.grocery.core.utilities.results.Result;
import com.example.grocery.core.utilities.results.SuccessDataResult;
import com.example.grocery.core.utilities.results.SuccessResult;
import com.example.grocery.dataAccess.abstracts.OrderRepository;
import com.example.grocery.entity.concretes.Order;
import com.example.grocery.entity.concretes.Product;
import com.example.grocery.webApi.requests.order.CreateOrderRequest;
import com.example.grocery.webApi.requests.order.DeleteOrderRequest;
import com.example.grocery.webApi.requests.order.UpdateOrderRequest;
import com.example.grocery.webApi.responses.order.GetAllOrderResponse;
import com.example.grocery.webApi.responses.order.GetByIdOrderResponse;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class OrderManager implements OrderService {

    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private MapperService mapperService;
    @Autowired
    private CustomerService customerService;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private ProductService productService;

    @Override
    @Transactional
    @CacheEvict(cacheNames = "order", allEntries = true)
    public Result add(CreateOrderRequest createOrderRequest) {

        Result rules = BusinessRules.run(isExistCustomerId(createOrderRequest.getCustomerId()),
                isExistPaymentId(createOrderRequest.getPaymentId()));
        if (!rules.isSuccess())
            return rules;

        Order order = mapperService.getModelMapper().map(createOrderRequest, Order.class);
        order.setCustomer(customerService.getCustomerById(createOrderRequest.getCustomerId()));
        order.setPayment(paymentService.getPaymentById(createOrderRequest.getPaymentId()));
        order.setProducts(productService.getProductsByIds(createOrderRequest.getProductIds()));
        orderRepository.save(order);
        log.info(LogInfoMessages.ORDER_CREATED, createOrderRequest.getCustomerId(), createOrderRequest.getPaymentId(),
                createOrderRequest.getProductIds());
        return new SuccessResult(CreateMessages.ORDER_CREATED);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "order", key = "#deleteOrderRequest.id")
    public Result delete(DeleteOrderRequest deleteOrderRequest) {

        Result rules = BusinessRules.run(isExistId(deleteOrderRequest.getId()));
        if (!rules.isSuccess())
            return rules;

        Order orderForLogging = orderRepository.findById(deleteOrderRequest.getId())
                .orElseThrow(() -> new BusinessException(ErrorMessages.ID_NOT_FOUND));

        Order order = mapperService.getModelMapper().map(deleteOrderRequest, Order.class);
        log.info(LogInfoMessages.ORDER_DELETED, orderForLogging.getId());
        orderRepository.delete(order);
        return new SuccessResult(DeleteMessages.ORDER_DELETED);
    }

    @Override
    @Transactional
    @CachePut(cacheNames = "order", key = "#id")
    public Result update(UpdateOrderRequest updateOrderRequest, Long id) {
        Order inDbOrder = orderRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorMessages.ID_NOT_FOUND));

        Result rules = BusinessRules.run(isExistId(id), isExistCustomerId(updateOrderRequest.getCustomerId()),
                isExistPaymentId(updateOrderRequest.getPaymentId()));
        if (!rules.isSuccess())
            return rules;

        Order order = mapperService.getModelMapper().map(updateOrderRequest, Order.class);
        order.setCustomer(customerService.getCustomerById(updateOrderRequest.getCustomerId()));
        order.setPayment(paymentService.getPaymentById(updateOrderRequest.getPaymentId()));
        order.setProducts(productService.getProductsByIds(updateOrderRequest.getProductIds()));
        order.setId(inDbOrder.getId());
        orderRepository.save(order);
        log.info(LogInfoMessages.ORDER_UPDATED, id, updateOrderRequest.getCustomerId(),
                updateOrderRequest.getPaymentId(),
                updateOrderRequest.getProductIds());
        return new SuccessResult(UpdateMessages.ORDER_UPDATED);
    }

    @Override
    @Cacheable(value = "order")
    public DataResult<List<GetAllOrderResponse>> getAll() {
        List<GetAllOrderResponse> returnList = new ArrayList<>();
        List<Order> orderList = orderRepository.findAll();
        for (Order order : orderList) {
            Order order1 = orderRepository.findById(order.getId())
                    .orElseThrow(() -> new BusinessException(ErrorMessages.ID_NOT_FOUND));
            GetAllOrderResponse addFields = mapperService.getModelMapper().map(order,
                    GetAllOrderResponse.class);
            addFields.setCustomerId(order1.getCustomer().getId());
            addFields.setPaymentId(order1.getPayment().getId());
            List<Long> ids = new ArrayList<>();
            for (Product x : order.getProducts()) {
                ids.add(x.getId());
            }
            addFields.setProductIds(ids);
            returnList.add(addFields);
        }
        return new SuccessDataResult<>(returnList, GetListMessages.ORDERS_LISTED);
    }

    @Override
    @Cacheable(value = "order", key = "#id")
    public DataResult<GetByIdOrderResponse> getById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorMessages.ID_NOT_FOUND));
        GetByIdOrderResponse getByIdOrderResponse = mapperService.getModelMapper().map(order,
                GetByIdOrderResponse.class);
        getByIdOrderResponse.setCustomerId(order.getCustomer().getId());
        getByIdOrderResponse.setPaymentId(order.getPayment().getId());
        List<Long> ids = new ArrayList<>();
        for (Product x : order.getProducts()) {
            ids.add(x.getId());
        }
        getByIdOrderResponse.setProductIds(ids);
        return new SuccessDataResult<>(getByIdOrderResponse, GetByIdMessages.ORDER_LISTED);
    }

    @Override
    @Cacheable(value = "order", key = "#sortBy")
    public DataResult<List<GetAllOrderResponse>> getListBySorting(String sortBy) {
        isValidSortParameter(sortBy);

        List<GetAllOrderResponse> returnList = new ArrayList<>();
        List<Order> orderList = orderRepository.findAll(Sort.by(Sort.Direction.ASC, sortBy));
        for (Order order : orderList) {
            Order order1 = orderRepository.findById(order.getId())
                    .orElseThrow(() -> new BusinessException(ErrorMessages.ID_NOT_FOUND));
            GetAllOrderResponse addFields = mapperService.getModelMapper().map(order,
                    GetAllOrderResponse.class);
            addFields.setCustomerId(order1.getCustomer().getId());
            addFields.setPaymentId(order1.getPayment().getId());
            List<Long> ids = new ArrayList<>();
            for (Product x : order.getProducts()) {
                ids.add(x.getId());
            }
            addFields.setProductIds(ids);
            returnList.add(addFields);
        }
        return new SuccessDataResult<>(returnList, GetListMessages.ORDERS_SORTED + sortBy);
    }

    @Override
    public DataResult<List<GetAllOrderResponse>> getListByPagination(int pageNo, int pageSize) {
        isPageNumberValid(pageNo);
        isPageSizeValid(pageSize);

        List<GetAllOrderResponse> returnList = new ArrayList<>();
        List<Order> orderList = orderRepository.findAll(PageRequest.of(pageNo, pageSize)).toList();
        for (Order order : orderList) {
            Order order1 = orderRepository.findById(order.getId())
                    .orElseThrow(() -> new BusinessException(ErrorMessages.ID_NOT_FOUND));
            GetAllOrderResponse addFields = mapperService.getModelMapper().map(order,
                    GetAllOrderResponse.class);
            addFields.setCustomerId(order1.getCustomer().getId());
            addFields.setPaymentId(order1.getPayment().getId());
            List<Long> ids = new ArrayList<>();
            for (Product x : order.getProducts()) {
                ids.add(x.getId());
            }
            addFields.setProductIds(ids);
            returnList.add(addFields);
        }
        return new SuccessDataResult<>(returnList, GetListMessages.ORDERS_PAGINATED);
    }

    @Override
    public DataResult<List<GetAllOrderResponse>> getListByPaginationAndSorting(int pageNo, int pageSize,
            String sortBy) {
        isPageNumberValid(pageNo);
        isPageSizeValid(pageSize);
        isValidSortParameter(sortBy);

        List<GetAllOrderResponse> returnList = new ArrayList<>();
        List<Order> orderList = orderRepository.findAll(PageRequest.of(pageNo, pageSize).withSort(Sort.by(sortBy)))
                .toList();
        for (Order order : orderList) {
            Order order1 = orderRepository.findById(order.getId())
                    .orElseThrow(() -> new BusinessException(ErrorMessages.ID_NOT_FOUND));
            GetAllOrderResponse addFields = mapperService.getModelMapper().map(order,
                    GetAllOrderResponse.class);
            addFields.setCustomerId(order1.getCustomer().getId());
            addFields.setPaymentId(order1.getPayment().getId());
            List<Long> ids = new ArrayList<>();
            for (Product x : order.getProducts()) {
                ids.add(x.getId());
            }
            addFields.setProductIds(ids);
            returnList.add(addFields);
        }
        return new SuccessDataResult<>(returnList, GetListMessages.ORDERS_PAGINATED_AND_SORTED + sortBy);
    }

    private Result isExistId(Long id) {
        if (!orderRepository.existsById(id)) {
            throw new BusinessException(ErrorMessages.ID_NOT_FOUND);
        }
        return new SuccessResult();
    }

    private Result isExistCustomerId(Long customerId) {
        if (customerService.getCustomerById(customerId) == null) {
            throw new BusinessException(ErrorMessages.CUSTOMER_ID_NOT_FOUND);
        }
        return new SuccessResult();
    }

    private Result isExistPaymentId(Long paymentId) {
        if (paymentService.getPaymentById(paymentId) == null) {
            throw new BusinessException(ErrorMessages.PAYMENT_ID_NOT_FOUND);
        }
        return new SuccessResult();
    }

    private void isPageNumberValid(int pageNo) {
        if (pageNo < 0) {
            log.warn(LogWarnMessages.PAGE_NUMBER_NEGATIVE);
            throw new BusinessException(ErrorMessages.PAGE_NUMBER_NEGATIVE);
        }
    }

    private void isPageSizeValid(int pageSize) {
        if (pageSize < 1) {
            log.warn(LogWarnMessages.PAGE_SIZE_NEGATIVE);
            throw new BusinessException(ErrorMessages.PAGE_SIZE_NEGATIVE);
        }
    }

    private void isValidSortParameter(String sortBy) {
        Order checkField = new Order();
        if (!checkField.toString().contains(sortBy)) {
            log.warn(LogWarnMessages.SORT_PARAMETER_NOT_VALID);
            throw new BusinessException(ErrorMessages.SORT_PARAMETER_NOT_VALID);
        }
    }

}
