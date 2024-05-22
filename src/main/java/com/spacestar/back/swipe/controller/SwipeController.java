package com.spacestar.back.swipe.controller;

import com.spacestar.back.global.ResponseEntity;
import com.spacestar.back.global.ResponseSuccess;
import com.spacestar.back.swipe.dto.req.SwipeReqDto;
import com.spacestar.back.swipe.dto.res.SwipeListResDto;
import com.spacestar.back.swipe.service.SwipeService;
import com.spacestar.back.swipe.vo.req.SwipeReqVo;
import com.spacestar.back.swipe.vo.res.SwipeListResVo;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/swipe")
public class SwipeController {
    private final SwipeService swipeService;
    private final ModelMapper mapper;
    @PostMapping("/add")
    public ResponseEntity<Void> addSwipe(@RequestHeader("uuid") String uuid,
                                         @RequestBody SwipeReqVo swipeReqVo){
        swipeService.addSwipe(mapper.map(swipeReqVo, SwipeReqDto.class),uuid);
        return new ResponseEntity<>(ResponseSuccess.SWIPE_ADD_SUCCESS);
    }

    @GetMapping
    public ResponseEntity<List<SwipeListResVo>> getSwipe(@RequestHeader("uuid") String uuid){
        List<SwipeListResDto> swipeListResDtos = swipeService.getSwipe(uuid);
        List<SwipeListResVo> SwipeListResVos = swipeListResDtos.stream()
                .map(dto -> mapper.map(dto, SwipeListResVo.class))
                .toList();

        return new ResponseEntity<>(ResponseSuccess.SWIPE_GET_SUCCESS,SwipeListResVos);
    }

    @PatchMapping("/agree")
    public ResponseEntity<Void> agreeSwipe(@RequestHeader("uuid") String uuid){
        swipeService.agreeSwipe(uuid);
        return new ResponseEntity<>(ResponseSuccess.SWIPE_AGREE_SUCCESS);
    }

    @PatchMapping("/reject")
    public ResponseEntity<Void> rejectSwipe(@RequestHeader("uuid") String uuid){
        swipeService.rejectSwipe(uuid);
        return new ResponseEntity<>(ResponseSuccess.SWIPE_REJECT_SUCCESS);
    }
}
