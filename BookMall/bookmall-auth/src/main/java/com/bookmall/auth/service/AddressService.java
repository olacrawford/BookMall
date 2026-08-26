package com.bookmall.auth.service;

import com.bookmall.auth.dto.AddressRequest;
import com.bookmall.auth.vo.AddressVO;

import java.util.List;

public interface AddressService {

    List<AddressVO> list(Long userId);

    AddressVO create(Long userId, AddressRequest request);

    AddressVO update(Long userId, Long addressId, AddressRequest request);

    void setDefault(Long userId, Long addressId);

    void delete(Long userId, Long addressId);
}