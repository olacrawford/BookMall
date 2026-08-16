package com.bookmall.address.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookmall.address.dto.AddressCreateRequest;
import com.bookmall.address.dto.AddressUpdateRequest;
import com.bookmall.address.entity.Address;
import com.bookmall.address.mapper.AddressMapper;
import com.bookmall.address.service.AddressService;
import com.bookmall.address.vo.AddressVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AddressServiceImpl implements AddressService {

    private final AddressMapper addressMapper;

    public AddressServiceImpl(AddressMapper addressMapper) {
        this.addressMapper = addressMapper;
    }

    @Override
    public List<AddressVO> listByUserId(Long userId) {
        List<Address> list = addressMapper.selectList(
                new LambdaQueryWrapper<Address>()
                        .eq(Address::getUserId, userId)
                        .orderByDesc(Address::getIsDefault)
                        .orderByDesc(Address::getUpdateTime)
        );
        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public AddressVO getById(Long id) {
        Address address = addressMapper.selectById(id);
        return address == null ? null : toVO(address);
    }

    @Override
    @Transactional
    public AddressVO createAddress(AddressCreateRequest request) {
        Address address = new Address();
        address.setUserId(request.getUserId());
        address.setReceiverName(request.getReceiverName());
        address.setReceiverPhone(request.getReceiverPhone());
        address.setProvince(request.getProvince());
        address.setCity(request.getCity());
        address.setDistrict(request.getDistrict());
        address.setDetailAddress(request.getDetailAddress());
        address.setIsDefault(request.getIsDefault() == null ? 0 : request.getIsDefault());
        address.setCreateTime(LocalDateTime.now());
        address.setUpdateTime(LocalDateTime.now());

        if (address.getIsDefault() == 1) {
            resetDefault(request.getUserId());
        } else {
            Long count = addressMapper.selectCount(
                    new LambdaQueryWrapper<Address>().eq(Address::getUserId, request.getUserId())
            );
            if (count == 0) {
                address.setIsDefault(1);
            }
        }

        addressMapper.insert(address);
        return toVO(address);
    }

    @Override
    @Transactional
    public AddressVO updateAddress(Long id, AddressUpdateRequest request) {
        Address address = addressMapper.selectById(id);
        if (address == null) {
            return null;
        }

        address.setReceiverName(request.getReceiverName());
        address.setReceiverPhone(request.getReceiverPhone());
        address.setProvince(request.getProvince());
        address.setCity(request.getCity());
        address.setDistrict(request.getDistrict());
        address.setDetailAddress(request.getDetailAddress());
        address.setIsDefault(request.getIsDefault() == null ? 0 : request.getIsDefault());
        address.setUpdateTime(LocalDateTime.now());

        if (address.getIsDefault() == 1) {
            resetDefault(address.getUserId());
        }

        addressMapper.updateById(address);
        return toVO(address);
    }

    @Override
    public boolean deleteAddress(Long id) {
        return addressMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional
    public boolean setDefault(Long id) {
        Address address = addressMapper.selectById(id);
        if (address == null) {
            return false;
        }

        resetDefault(address.getUserId());
        address.setIsDefault(1);
        address.setUpdateTime(LocalDateTime.now());
        addressMapper.updateById(address);
        return true;
    }

    private void resetDefault(Long userId) {
        List<Address> addresses = addressMapper.selectList(
                new LambdaQueryWrapper<Address>().eq(Address::getUserId, userId)
        );
        for (Address item : addresses) {
            if (item.getIsDefault() != null && item.getIsDefault() == 1) {
                item.setIsDefault(0);
                item.setUpdateTime(LocalDateTime.now());
                addressMapper.updateById(item);
            }
        }
    }

    private AddressVO toVO(Address address) {
        AddressVO vo = new AddressVO();
        vo.setId(address.getId());
        vo.setUserId(address.getUserId());
        vo.setReceiverName(address.getReceiverName());
        vo.setReceiverPhone(address.getReceiverPhone());
        vo.setProvince(address.getProvince());
        vo.setCity(address.getCity());
        vo.setDistrict(address.getDistrict());
        vo.setDetailAddress(address.getDetailAddress());
        vo.setIsDefault(address.getIsDefault());
        vo.setCreateTime(address.getCreateTime());
        vo.setUpdateTime(address.getUpdateTime());
        return vo;
    }
}