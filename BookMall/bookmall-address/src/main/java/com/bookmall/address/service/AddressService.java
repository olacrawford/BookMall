package com.bookmall.address.service;

import com.bookmall.address.dto.AddressCreateRequest;
import com.bookmall.address.dto.AddressUpdateRequest;
import com.bookmall.address.vo.AddressVO;

import java.util.List;

public interface AddressService {

    List<AddressVO> listByUserId(Long userId);

    AddressVO getById(Long id);

    AddressVO createAddress(AddressCreateRequest request);

    AddressVO updateAddress(Long id, AddressUpdateRequest request);

    boolean deleteAddress(Long id);

    boolean setDefault(Long id);
}