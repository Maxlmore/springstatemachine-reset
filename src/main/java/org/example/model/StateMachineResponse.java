package org.example.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.example.config.States;

import java.util.Collection;

@Getter
@Setter
@AllArgsConstructor
public class StateMachineResponse {

    Collection<States> states;
}
