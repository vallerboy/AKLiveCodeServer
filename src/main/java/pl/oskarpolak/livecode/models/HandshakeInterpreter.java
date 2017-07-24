package pl.oskarpolak.livecode.models;


import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;
import org.springframework.web.util.UriTemplate;

import java.util.Map;

@Configuration
public class HandshakeInterpreter extends HttpSessionHandshakeInterceptor {

    private static final UriTemplate URI_TEMPLATE = new UriTemplate("/live/{roomId}");

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        Map<String, String> segments = URI_TEMPLATE.match(request.getURI().getPath());

        attributes.put("roomId", segments.get("roomId"));

        return super.beforeHandshake(request, response, wsHandler, attributes);
    }
}