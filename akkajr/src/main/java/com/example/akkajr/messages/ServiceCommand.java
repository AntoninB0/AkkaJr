package com.example.akkajr.messages;

import com.example.akkajr.core.MySerializable;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Interface de base pour tous les messages de commande.
 * Hérite de MySerializable pour activer la sérialisation Cluster.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public interface ServiceCommand extends MySerializable {
    String getServiceId();
}