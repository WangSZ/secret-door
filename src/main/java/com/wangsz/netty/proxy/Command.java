package com.wangsz.netty.proxy;

import java.util.List;

public class Command {

    public static final String login="login";
    public static final String register="register";
    public static final String newTunnel="new-tunnel";
    public static final String updateConfig="update-config";
    public static final String clientStart="client-start";
    String name;
    List<String> args;

    public Command(String name) {
        this.name = name;
    }

    public Command(String name, List<String> args) {
        this.name = name;
        this.args = args;
    }

    public Command() {
    }

    @Override
    public String toString() {
        return "Command{" +
                "name='" + name + '\'' +
                ", args=" + args +
                '}';
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getArgs() {
        return args;
    }

    public void setArgs(List<String> args) {
        this.args = args;
    }

    public static final Command ping=new Command("ping");
    public static final Command pong=new Command("pong");
}
