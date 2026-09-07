package com.kh.midpoint;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;

import com.kh.midpoint.chat.model.vo.ChatSession;

class ConstantPropertiesTests {
    @Test
    void springInjectsConstantsIntoEveryConsumer() throws Exception {
        try (var context = new org.springframework.context.annotation.AnnotationConfigApplicationContext()) {
            context.getEnvironment().getPropertySources().addFirst(
                new org.springframework.core.env.PropertiesPropertySource("constants", constants()));
            context.refresh();
            String[] consumers = {
                "game.model.service.GameService", "vote.model.service.ModeVoteService",
                "external.tmap.TmapRouteClient", "external.kakao.KakaoTransitClient",
                "common.config.WebSocketConfig", "chat.controller.ChatController",
                "game.controller.GameSocketController", "game.controller.GameDisconnectListener",
                "vote.controller.ModeVoteSocketController", "route.controller.RouteSocketController",
                "point.controller.MidPointController"
            };
            for (String name : consumers) {
                Class<?> type = Class.forName("com.kh.midpoint." + name);
                Object bean = org.mockito.Mockito.mock(type);
                context.getAutowireCapableBeanFactory().autowireBean(bean);
                for (var field : type.getDeclaredFields()) {
                    var annotation = field.getAnnotation(org.springframework.beans.factory.annotation.Value.class);
                    if (annotation == null) continue;
                    String key = annotation.value().substring(2, annotation.value().length() - 1);
                    field.setAccessible(true);
                    assertEquals(context.getEnvironment().getProperty(key, field.getType()), field.get(bean), name + "." + field.getName());
                }
            }
        }
    }

    private Properties constants() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application-constant.yml"));
        return yaml.getObject();
    }

    @Test
    void migratedValuesPreserveExistingContracts() {
        Properties values = constants();
        Map<String, String> expected = Map.ofEntries(
            Map.entry("game.status.playing", "PLAYING"),
            Map.entry("game.status.finished", "FINISHED"),
            Map.entry("game.status.aborted", "ABORTED"),
            Map.entry("room.stage.resolving", "RESOLVING"),
            Map.entry("room.stage.game-playing", "GAME_PLAYING"),
            Map.entry("vote.mode.game", "GAME"),
            Map.entry("vote.mode.random", "RANDOM"),
            Map.entry("route.segment-type.walking", "WALKING"),
            Map.entry("route.segment-type.bus", "BUS"),
            Map.entry("route.segment-type.subway", "SUBWAY"),
            Map.entry("chat.session-attribute-key", "chatSession"));
        expected.forEach((key, value) -> assertEquals(value, values.getProperty(key), key));
        assertEquals("30", values.getProperty("game.turn.seconds"));
        assertEquals("WALK", values.getProperty("route.mode.walk"));
        assertEquals("10000", values.getProperty("chat.heartbeat-interval"));
    }

    @Test
    void sessionRoundTripAndMissingSessionBehaviorArePreserved() {
        String key = constants().getProperty("chat.session-attribute-key");
        ChatSession session = new ChatSession("room", 1L, 2L, "nickname");
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        assertNull(ChatSession.from(accessor, key));
        accessor.setSessionAttributes(Map.of());
        assertNull(ChatSession.from(accessor, key));
        accessor.setSessionAttributes(Map.of(key, session));
        assertSame(session, ChatSession.from(accessor, key));
    }
}
