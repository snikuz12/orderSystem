package beyond.ordersystem.member.controller;

import beyond.ordersystem.common.auth.JwtTokenProvider;
import beyond.ordersystem.common.dto.CommonErrorDto;
import beyond.ordersystem.common.dto.CommonResDto;
import beyond.ordersystem.common.service.CommonExceptionHandler;
import beyond.ordersystem.member.domain.Member;
import beyond.ordersystem.member.dto.MemberCreateReqDto;
import beyond.ordersystem.member.dto.MemberListResDto;
import beyond.ordersystem.member.dto.MemberLoginDto;
import beyond.ordersystem.member.dto.MemberRefreshDto;
import beyond.ordersystem.member.service.MemberService;
import com.fasterxml.classmate.MemberResolver;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/member")
@Slf4j
public class MemberController {

    private final CommonExceptionHandler commonExceptionHandler;
    @Value("${jwt.secretKeyRt}")
    private String secretKeyRt;

    //private static final Logger log = LoggerFactory.getLogger(MemberController.class);
    private final MemberService memberService;
    private final JwtTokenProvider jwtTokenProvider;

    @Qualifier("2")
    private final RedisTemplate<String, Object> redisTemplate;

    @Autowired
    public MemberController (MemberService memberService, JwtTokenProvider jwtTokenProvider, @Qualifier("2") RedisTemplate<String, Object> template, CommonExceptionHandler commonExceptionHandler) {
        this.memberService = memberService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.redisTemplate = template;
        this.commonExceptionHandler = commonExceptionHandler;
    }

    /**
     * 회원 가입
     */
    @PostMapping("/create")
    public ResponseEntity<?> memberCreate(@Valid @RequestBody MemberCreateReqDto dto) {

//        try {
            log.info("controller: " + dto.getEmail());
            Member member = memberService.createMember(dto);

            CommonResDto commonResDto = new CommonResDto(HttpStatus.CREATED, "member created successfully", member.getId());
            return new ResponseEntity<>(commonResDto, HttpStatus.CREATED);
//        } catch (IllegalArgumentException e) {
//
//            CommonErrorDto commonErrorDto = new CommonErrorDto(HttpStatus.BAD_REQUEST.value(), e.getMessage());
//            return new ResponseEntity<>(commonErrorDto, HttpStatus.BAD_REQUEST);
//        }
    }


    // admin만 회원목록전체조회 가능
    /**
     * 회원 목록 조회
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/list")
    public ResponseEntity<?> memberList(@PageableDefault(size=10, sort = "createdTime"
            , direction = Sort.Direction.DESC) Pageable pageable) {

//        try {
            Page<MemberListResDto> memberListResDtos = memberService.memberList(pageable);

            CommonResDto commonResDto = new CommonResDto(HttpStatus.OK, "member are found", memberListResDtos);
            return new ResponseEntity<>(commonResDto, HttpStatus.OK);
//        } catch (IllegalArgumentException e) {
//
//            CommonErrorDto commonErrorDto = new CommonErrorDto(HttpStatus.BAD_REQUEST.value(), e.getMessage());
//            return new ResponseEntity<>(commonErrorDto, HttpStatus.BAD_REQUEST);
//        }
    }

    // 본인은 본인회원정보만 조회가능
    // /member/myinfo. MemberResDto
    @GetMapping("/member/myinfo")
    public ResponseEntity<?> memberMyInfo(){
        MemberListResDto dto = memberService.myInfo();
        CommonResDto commonResDto = new CommonResDto(HttpStatus.OK,"member is found",dto);
        return new ResponseEntity<>(commonResDto, HttpStatus.OK);
    }

    /**
     * 로그인
     */
    @PostMapping("/doLogin")
    public ResponseEntity doLogin(@RequestBody MemberLoginDto dto) {

        // email, password가 일치한지 검증
        Member member = memberService.login(dto);

        // 일치할 경우 accessToken 생성
        String jwtToken = jwtTokenProvider.createToken(member.getEmail(), member.getRole().toString());
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getEmail(), member.getRole().toString());

        // redis에 email과 rt를 key:value로 하여 저장
        redisTemplate.opsForValue().set(member.getEmail(), refreshToken, 240, TimeUnit.HOURS); // 240시간
        // 생성된 토큰을 commonResDto에 담아 사용자에게 return
        Map<String, Object> loginInfo = new HashMap<>();
        loginInfo.put("id", member.getId());
        loginInfo.put("token", jwtToken);
        loginInfo.put("refreshToken", refreshToken);
        CommonResDto commonResDto = new CommonResDto(HttpStatus.OK, "login is successful", loginInfo);
        return new ResponseEntity<>(commonResDto, HttpStatus.OK);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> generateNewAccessToken(@RequestBody MemberRefreshDto dto) {
        String rt = dto.getRefreshToken();
        Claims claims = null;
        try{
            // 코드를 통해 rt 검증
            claims = Jwts.parser().setSigningKey(secretKeyRt).parseClaimsJws(rt).getBody();
        }catch (Exception e){
            return new ResponseEntity<>(new CommonErrorDto(HttpStatus.UNAUTHORIZED.value(), "invalid refresh token"), HttpStatus.UNAUTHORIZED);
        }

        String email = claims.getSubject();
        String role = claims.get("role").toString();

        // redis를 조회하여 rt 추가 검증
        Object obj = redisTemplate.opsForValue().get(email);
        if(obj == null || !obj.toString().equals(role)){
            return new ResponseEntity<>(new CommonErrorDto(HttpStatus.UNAUTHORIZED.value(), "invalid refresh token"), HttpStatus.UNAUTHORIZED);
        }


        String newAt = jwtTokenProvider.createToken(email, role);
        // 생성된 토큰을 commonResDto에 담아 사용자에게 return
        Map<String, Object> info = new HashMap<>();
        info.put("token", newAt);
        CommonResDto commonResDto = new CommonResDto(HttpStatus.OK, "at is renewed", info);
        return new ResponseEntity<>(commonResDto, HttpStatus.OK);
    }



}
