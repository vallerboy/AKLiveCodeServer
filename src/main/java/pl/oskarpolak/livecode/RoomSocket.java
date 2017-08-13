package pl.oskarpolak.livecode;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;
import pl.oskarpolak.livecode.models.HandshakeInterpreter;
import pl.oskarpolak.livecode.models.MessageModel;
import pl.oskarpolak.livecode.models.Room;
import pl.oskarpolak.livecode.models.User;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Configuration
@EnableWebSocket
public class RoomSocket extends BinaryWebSocketHandler implements WebSocketConfigurer {

    private static final Gson gson = new GsonBuilder().create();

    public List<Room> roomList = Collections.synchronizedList(new ArrayList<Room>());


    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(this, "/live/{roomId}")
                .setAllowedOrigins("*").addInterceptors(new HandshakeInterpreter());

    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        Type type = new TypeToken<MessageModel>(){}.getType();
        MessageModel messageModel = gson.fromJson(new String(message.getPayload().array()),  type);


        String roomName = (String) session.getAttributes().get("roomId");

        Optional<Room> roomExist = roomList.stream()
                .filter(s -> s.getName().equals(roomName))
                .findAny();

        Room room = roomExist.get();




        Optional<User> user = room.getUserBySessionId(session.getId());

        user.ifPresent(s -> System.out.println("User jest!"));

        switch (messageModel.getMessageType()){
            case REGISTER: {
                    String nick = messageModel.getContext();
                    if(user.isPresent()){
                        MessageModel registerResponse = new MessageModel();
                        registerResponse.setMessageType(MessageModel.MessageType.REGISTER_RESPONSE);

                        if(room.getUserList().contains(nick)){
                            registerResponse.setContext("nickexist");
                            sendMessage(registerResponse, room.getUserByName(user.get().getName()));
                            break;
                        }
                        user.get().setName(nick);
                        registerResponse.setContext("Pomyslnie ustawiono nick");
                        sendMessage(registerResponse, room.getUserByName(user.get().getName()));

                        MessageModel joinMessage = new MessageModel();
                        joinMessage.setMessageType(MessageModel.MessageType.JOIN);
                        joinMessage.setContext(nick);
                        sendToAllWithoutMe(joinMessage, user.get(), room);
                    }

                    break;
            }
            case REQUEST_ALL_USER: {
                    List<String> users = room.getUserList().stream()
                             .filter(s -> s.getName() != null)
                            .map(s -> s.getName())
                            .collect(Collectors.toList());


                    MessageModel model = new MessageModel();
                    model.setMessageType(MessageModel.MessageType.REQUEST_ALL_USER);
                    model.setContext(gson.toJson(users));

                    sendMessage(model, user.get());
                    break;
            }
            case DOWNLOAD_REQUEST: {
                MessageModel model = new MessageModel();
                model.setMessageType(MessageModel.MessageType.DOWNLOAD_REQUEST);
                model.setToWho(room.getUserBySessionId(session.getId()).get().getName());
                sendMessage(model, room.getUserByName(messageModel.getToWho()));
                break;
            }

            case DOWNLOAD_RESPONSE:{
                MessageModel model = new MessageModel();
                model.setMessageType(MessageModel.MessageType.DOWNLOAD_RESPONSE);
                model.setContext(messageModel.getContext());
                System.out.println("Context: " + messageModel.getContext());
                sendMessage(model, room.getUserByName(messageModel.getToWho()));
                break;
            }


            case UPDATING:{
                MessageModel model = new MessageModel();
                model.setMessageType(MessageModel.MessageType.UPDATING);
                model.setContext(messageModel.getContext());
                model.setToWho(user.get().getName());
                sendToAllWithoutMe(model, user.get(), room);
                break;
            }
        }



    }

    private void sendMessage(MessageModel messageModel, User user){
        String message = gson.toJson(messageModel);
        try {
            user.getSession().sendMessage(new BinaryMessage(ByteBuffer.wrap(message.getBytes())));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(MessageModel messageModel, WebSocketSession session){
        String message = gson.toJson(messageModel);
        try {
            session.sendMessage(new BinaryMessage(ByteBuffer.wrap(message.getBytes())));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendMessageToAll(MessageModel messageModel, Room room){
        room.getUserList().forEach(s -> sendMessage(messageModel, s));
    }

    private void sendToAllWithoutMe(MessageModel messageModel, User user, Room room){
        room.getUserList().stream()
                .filter(s -> !s.getSessionId().equals(user.getSessionId()))
                .forEach(s -> sendMessage(messageModel, s));
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String roomName = (String) session.getAttributes().get("roomId");

        Optional<Room> roomExist = roomList.stream()
                .filter(s -> s.getName().equals(roomName))
                .findAny();

        Room room;
        if(!roomExist.isPresent()){
            room = new Room(roomName);
            roomList.add(room);
            System.out.println("Utworzono nowy pokój: " + roomName);
        }
        roomList.stream()
                .filter(s -> s.getName().equals(roomName))
                .findAny()
                .ifPresent(s -> {
                    if(!s.getUserBySessionId(session.getId()).isPresent()){
                        s.addUser(new User(session));
                        System.out.println("Dodaje usera");
                    }
                });

        System.out.println("SESJA: " + session.getAttributes().get("roomId"));
        System.out.println("SESJA: " + session.getHandshakeHeaders().get("roomId"));

    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        roomList.stream()
                .filter(s -> s.getName().equals(session.getAttributes().get("roomId")))
                .findAny()
                .ifPresent(s -> {
                    if(s.getUserBySessionId(session.getId()).isPresent()){
                        sendDisconnect(s, s.getUserBySessionId(session.getId()).get().getName());
                        s.removeUser(s.getUserBySessionId(session.getId()).get());
                    }
                });
        System.out.println("Odchodzi user");
    }

    private void sendDisconnect(Room room, String name) {
        MessageModel messageModel = new MessageModel();
        messageModel.setMessageType(MessageModel.MessageType.DISCONNECT);
        messageModel.setToWho(name);
        sendToAllWithoutMe(messageModel, room.getUserByName(name), room);
    }
}
