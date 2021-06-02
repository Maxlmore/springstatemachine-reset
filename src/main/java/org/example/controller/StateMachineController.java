package org.example.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.config.Events;
import org.example.config.States;
import org.example.model.StateMachineResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.StateMachineEventResult;
import org.springframework.statemachine.persist.StateMachinePersister;
import org.springframework.statemachine.state.PseudoStateKind;
import org.springframework.statemachine.support.DefaultExtendedState;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.servlet.http.HttpServletRequest;

@Controller
public class StateMachineController {

    @Autowired
    private StateMachine<States, Events> stateMachine;

    @Autowired
    private StateMachinePersister<States, Events, String> stateMachinePersister;

    @Autowired
    RedisTemplate<String, String> redisTemplate;

    @GetMapping("/state")
    @ResponseBody
    public String state(HttpServletRequest httpServletRequest) throws Exception {
        resetStateMachineFromStore(httpServletRequest.getSession().getId());
        return convertToJson(buildGetStateResponse());
    }

    @GetMapping("/event")
    @ResponseBody
    public String event(@RequestParam(value = "eventName") String eventName,
                        HttpServletRequest httpServletRequest) throws Exception {
        resetStateMachineFromStore(httpServletRequest.getSession().getId());

        feedMachine(Mono.just(
                MessageBuilder.withPayload(Events.valueOf(eventName)).build()),
                httpServletRequest.getSession().getId())
                .subscribe();
        return convertToJson(buildGetStateResponse());
    }

    private Flux<StateMachineEventResult<States, Events>> feedMachine(Mono<Message<Events>> message, String identifier) {
        return stateMachine.sendEvent(message)
                .doOnComplete(() -> {
                    try {
                        if (stateMachine.getState().getPseudoState() != null && stateMachine.getState().getPseudoState().getKind() == PseudoStateKind.END) {
                            redisTemplate.delete("spring:session:statemachine:" + identifier);
                        } else {
                            stateMachinePersister.persist(stateMachine, "spring:session:statemachine:" + identifier);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
    }

    private void resetStateMachineFromStore(String identifier) throws Exception {
        stateMachinePersister.restore(stateMachine, "spring:session:statemachine:" + identifier);
    }

    private StateMachineResponse buildGetStateResponse() {
        return new StateMachineResponse(
                stateMachine.getState().getIds());
    }

    private String convertToJson(StateMachineResponse getStateResponse) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(getStateResponse);
    }
}
