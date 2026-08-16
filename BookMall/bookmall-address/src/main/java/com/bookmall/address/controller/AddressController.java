package com.bookmall.address.controller;

import com.bookmall.address.dto.AddressCreateRequest;
import com.bookmall.address.dto.AddressUpdateRequest;
import com.bookmall.address.service.AddressService;
import com.bookmall.address.vo.AddressVO;
import com.bookmall.common.result.Result;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/address")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping("/hello")
    public Result<String> hello() {
        return Result.success("bookmall-address is running");
    }

    @GetMapping("/list")
    public Result<List<AddressVO>> list(@RequestParam("userId") Long userId) {
        return Result.success(addressService.listByUserId(userId));
    }

    @GetMapping("/{id}")
    public Result<AddressVO> getById(@PathVariable("id") Long id) {
        AddressVO address = addressService.getById(id);
        if (address == null) {
            return Result.fail(404, "地址不存在");
        }
        return Result.success(address);
    }

    @PostMapping
    public Result<AddressVO> create(@Valid @RequestBody AddressCreateRequest request) {
        return Result.success(addressService.createAddress(request));
    }

    @PutMapping("/{id}")
    public Result<AddressVO> update(@PathVariable("id") Long id,
                                    @Valid @RequestBody AddressUpdateRequest request) {
        AddressVO address = addressService.updateAddress(id, request);
        if (address == null) {
            return Result.fail(404, "地址不存在");
        }
        return Result.success(address);
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable("id") Long id) {
        boolean deleted = addressService.deleteAddress(id);
        if (!deleted) {
            return Result.fail(404, "地址不存在");
        }
        return Result.success("删除成功");
    }

    @PutMapping("/{id}/default")
    public Result<String> setDefault(@PathVariable("id") Long id) {
        boolean success = addressService.setDefault(id);
        if (!success) {
            return Result.fail(404, "地址不存在");
        }
        return Result.success("设置默认地址成功");
    }
}