package com.example.akkajr.core;

import org.springframework.stereotype.Component;

public class TestService extends Service {
    
    private boolean shouldCrash = false;
    private int executionCount = 0;
    
    public TestService(String name) {
        super(name);
    }
    
    @Override
    protected void onStart() throws Exception {
        logger.info("🚀 Démarrage de " + name);
    }
    
    @Override
    protected void onStop() throws Exception {
        logger.info("🛑 Arrêt de " + name);
    }
    
    @Override
    public  void execute() throws Exception {
        for (String command : inputsCommands) {
            executionCount++;
            logger.info("⚙️ Exécution commande #" + executionCount + " : " + command);
            
            // Simule un travail
            Thread.sleep(2000);
            
            // Simule un crash
            if (shouldCrash && executionCount == 3) {
                logger.severe("💥 CRASH SIMULÉ !");
                // Arrête d'envoyer des heartbeats
                while (true) {
                    Thread.sleep(1000);
                }
            }
            
            ping();  // Heartbeat après chaque commande
        }
        
        clearCommands();
    }
    
    @Override
    protected boolean validateConfiguration() {
        return true;
    }
    
    public void setCrashMode(boolean shouldCrash) {
        this.shouldCrash = shouldCrash;
    }
}