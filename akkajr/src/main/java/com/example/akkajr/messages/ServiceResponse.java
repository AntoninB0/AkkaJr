package com.example.akkajr.messages;

import com.example.akkajr.core.MySerializable;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public interface ServiceResponse extends MySerializable {
}