package com.spacestar.back.quickmatching.controller;

import com.spacestar.back.quickmatching.service.QuickMatchingService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/quick-matching")
@RequiredArgsConstructor
public class QuickMatchingController {
    private final QuickMatchingService quickMatchingService;
    private final ModelMapper mapper;

//    @PostMapping
//    public ResponseEntity<?> enterMatchQueue(@RequestHeader("UUID")String uuid,
//                                             @RequestBody MatchQueueReqVo reqVo){
//        quickMatchingService.enterMatchQueue(uuid,mapper.map(reqVo, MatchQueueReqDto.class));
//        return null;
//    }
}
