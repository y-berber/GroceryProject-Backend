package com.example.grocery.business.concretes;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.example.grocery.business.abstracts.SupplierService;
import com.example.grocery.business.constants.Messages.CreateMessages;
import com.example.grocery.business.constants.Messages.DeleteMessages;
import com.example.grocery.business.constants.Messages.ErrorMessages;
import com.example.grocery.business.constants.Messages.GetByIdMessages;
import com.example.grocery.business.constants.Messages.GetListMessages;
import com.example.grocery.business.constants.Messages.UpdateMessages;
import com.example.grocery.business.constants.Messages.LogMessages.LogInfoMessages;
import com.example.grocery.business.constants.Messages.LogMessages.LogWarnMessages;
import com.example.grocery.core.utilities.business.BusinessRules;
import com.example.grocery.core.utilities.exceptions.BusinessException;
import com.example.grocery.core.utilities.mapper.MapperService;
import com.example.grocery.core.utilities.results.DataResult;
import com.example.grocery.core.utilities.results.Result;
import com.example.grocery.core.utilities.results.SuccessDataResult;
import com.example.grocery.core.utilities.results.SuccessResult;
import com.example.grocery.dataAccess.abstracts.SupplierRepository;
import com.example.grocery.entity.concretes.Supplier;
import com.example.grocery.webApi.requests.supplier.CreateSupplierRequest;
import com.example.grocery.webApi.requests.supplier.DeleteSupplierRequest;
import com.example.grocery.webApi.requests.supplier.UpdateSupplierRequest;
import com.example.grocery.webApi.responses.supplier.GetAllSupplierResponse;
import com.example.grocery.webApi.responses.supplier.GetByIdSupplierResponse;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SupplierManager implements SupplierService {

    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private MapperService mapperService;

    @Override
    public Result add(CreateSupplierRequest createSupplierRequest) {

        Result rules = BusinessRules.run(isExistEmail(createSupplierRequest.getEmail()),
                isExistName(createSupplierRequest.getName()),
                isExistPhoneNumber(createSupplierRequest.getPhoneNumber()));
        if (!rules.isSuccess())
            return rules;

        Supplier supplier = mapperService.getModelMapper().map(createSupplierRequest, Supplier.class);
        supplierRepository.save(supplier);
        log.info(LogInfoMessages.SUPPLIER_ADDED, createSupplierRequest.getName());
        return new SuccessResult(CreateMessages.SUPPLIER_CREATED);
    }

    @Override
    public Result update(UpdateSupplierRequest updateSupplierRequest, Long id) {

        Result rules = BusinessRules.run(isExistEmail(updateSupplierRequest.getEmail()),
                isExistName(updateSupplierRequest.getName()),
                isExistPhoneNumber(updateSupplierRequest.getPhoneNumber()),
                isExistId(id));
        if (!rules.isSuccess())
            return rules;

        var inDbSupplier = supplierRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorMessages.ID_NOT_FOUND));
        Supplier supplier = mapperService.getModelMapper().map(updateSupplierRequest, Supplier.class);
        supplier.setId(inDbSupplier.getId());
        supplierRepository.save(supplier);
        log.info(LogInfoMessages.SUPPLIER_UPDATED, updateSupplierRequest.getName());
        return new SuccessResult(UpdateMessages.SUPPLIER_MODIFIED);
    }

    @Override
    public Result delete(DeleteSupplierRequest deleteSupplierRequest) {

        Result rules = BusinessRules.run(isExistId(deleteSupplierRequest.getId()));
        if (!rules.isSuccess())
            return rules;

        Supplier supplier = mapperService.getModelMapper().map(deleteSupplierRequest, Supplier.class);
        log.info(LogInfoMessages.SUPPLIER_DELETED, getSupplierById(deleteSupplierRequest.getId()).getName());
        supplierRepository.delete(supplier);
        return new SuccessResult(DeleteMessages.SUPPLIER_DELETED);
    }

    @Override
    public DataResult<List<GetAllSupplierResponse>> getAll() {
        List<Supplier> suppliers = supplierRepository.findAll();
        List<GetAllSupplierResponse> returnList = suppliers.stream()
                .map(s -> mapperService.getModelMapper().map(s, GetAllSupplierResponse.class)).toList();
        return new SuccessDataResult<>(returnList, GetListMessages.SUPPLIERS_LISTED);
    }

    @Override
    public DataResult<GetByIdSupplierResponse> getById(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorMessages.ID_NOT_FOUND));
        GetByIdSupplierResponse getByIdSupplierResponse = mapperService.getModelMapper().map(supplier,
                GetByIdSupplierResponse.class);
        return new SuccessDataResult<>(getByIdSupplierResponse, GetByIdMessages.SUPPLIER_LISTED);
    }

    @Override
    public DataResult<List<GetAllSupplierResponse>> getListBySorting(String sortBy) {
        isValidSortParameter(sortBy);

        List<Supplier> suppliers = supplierRepository.findAll(Sort.by(Sort.Direction.ASC, sortBy));
        List<GetAllSupplierResponse> returnList = suppliers.stream()
                .map(s -> mapperService.getModelMapper().map(s, GetAllSupplierResponse.class)).toList();
        return new SuccessDataResult<>(returnList, GetListMessages.SUPPLIERS_SORTED + sortBy);
    }

    @Override
    public DataResult<List<GetAllSupplierResponse>> getListByPagination(int pageNo, int pageSize) {
        isPageNumberValid(pageNo);
        isPageSizeValid(pageSize);

        List<Supplier> suppliers = supplierRepository.findAll(PageRequest.of(pageNo, pageSize)).toList();
        List<GetAllSupplierResponse> returnList = suppliers.stream()
                .map(s -> mapperService.getModelMapper().map(s, GetAllSupplierResponse.class)).toList();
        return new SuccessDataResult<>(returnList, GetListMessages.SUPPLIERS_PAGINATED);
    }

    @Override
    public DataResult<List<GetAllSupplierResponse>> getListByPaginationAndSorting(int pageNo, int pageSize,
            String sortBy) {
        isPageNumberValid(pageNo);
        isPageSizeValid(pageSize);
        isValidSortParameter(sortBy);

        List<Supplier> suppliers = supplierRepository
                .findAll(PageRequest.of(pageNo, pageSize).withSort(Sort.by(sortBy))).toList();
        List<GetAllSupplierResponse> returnList = suppliers.stream()
                .map(s -> mapperService.getModelMapper().map(s, GetAllSupplierResponse.class)).toList();
        return new SuccessDataResult<>(returnList, GetListMessages.SUPPLIERS_PAGINATED_AND_SORTED + sortBy);
    }

    // ProductManager sınıfımızda bağımlılığı kontrol altına alma adına kullanılmak
    // üzere tasarlandı.
    @Override
    public Supplier getSupplierById(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorMessages.SUPPLIER_ID_NOT_FOUND));
    }

    private Result isExistId(Long id) {
        if (!supplierRepository.existsById(id)) {
            throw new BusinessException(ErrorMessages.ID_NOT_FOUND);
        }
        return new SuccessResult();
    }

    private Result isExistName(String name) {
        if (supplierRepository.existsByName(name)) {
            log.warn(LogWarnMessages.SUPPLIER_NAME_REPEATED, name);
            throw new BusinessException(ErrorMessages.SUPPLIER_NAME_REPEATED);
        }
        return new SuccessResult();
    }

    private Result isExistEmail(String email) {
        if (supplierRepository.existsByEmail(email)) {
            log.warn(LogWarnMessages.SUPPLIER_EMAIL_REPEATED, email);
            throw new BusinessException(ErrorMessages.EMAIL_REPEATED);
        }
        return new SuccessResult();
    }

    private Result isExistPhoneNumber(String phoneNumber) {
        if (supplierRepository.existsByPhoneNumber(phoneNumber)) {
            log.warn(LogWarnMessages.SUPPLIER_PHONE_NUMBER_REPEATED, phoneNumber);
            throw new BusinessException(ErrorMessages.PHONE_NUMBER_REPEATED);
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
        Supplier checkField = new Supplier();
        if (!checkField.toString().contains(sortBy)) {
            log.warn(LogWarnMessages.SORT_PARAMETER_NOT_VALID);
            throw new BusinessException(ErrorMessages.SORT_PARAMETER_NOT_VALID);
        }
    }

}
