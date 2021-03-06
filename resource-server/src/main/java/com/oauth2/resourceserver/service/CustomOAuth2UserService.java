package com.oauth2.resourceserver.service;


import com.oauth2.resourceserver.dao.UserDao;
import com.oauth2.resourceserver.vo.OAuthAttributes;
import com.oauth2.resourceserver.vo.UserVO;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.util.Collections;

@Log4j2
@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {
    private final UserDao userDao;
    private final HttpSession httpSession;

    public CustomOAuth2UserService(UserDao userDao, HttpSession httpSession) {
        this.userDao = userDao;
        this.httpSession = httpSession;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2UserService delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        OAuthAttributes attributes = OAuthAttributes.
                of(registrationId, userNameAttributeName, oAuth2User);

        if (attributes==null)
            throw new NullPointerException();

        UserVO user = null;
        try {
            user = save(attributes);
            log.info(user.toString());
        } catch (Exception e) {
            log.error(e);
            return null;
        }
        httpSession.setAttribute("user", user);

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(String.valueOf(user.getRoles()))),
                attributes.getAttributes(),
                attributes.getNameAttributeKey());
    }

    private UserVO save(OAuthAttributes attributes) throws Exception {
        UserVO user = UserVO.builder()
                .uid(attributes.getId())
                .name(attributes.getName())
                .roles(String.valueOf(attributes.getRole()))
                .build();

        userDao.insertData(user);
        return user;
    }
}
