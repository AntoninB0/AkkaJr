package com.example.akkajr.messaging;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.example.akkajr.messaging.dispatcher.Dispatcher;  // ADD THIS LINE
import com.example.akkajr.messaging.mailbox.DeadLetterMailbox;
import com.example.akkajr.messaging.mailbox.Mailbox;

class DispatcherTest {

    private Dispatcher dispatcher;
    
    @Mock
    private DeadLetterMailbox deadLetters;
    
    @Mock
    private MessageService messageService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        dispatcher = new Dispatcher(deadLetters, messageService);
    }

    @Test
    void testDispatchTellMessage() {
        // Arrange
        Message msg = new Message("agentA", "agentB", "Test");
        Mailbox mailbox = new Mailbox();
        
        // Act
        dispatcher.dispatch(msg, mailbox);
        
        // Assert - Should not throw, message should be processed asynchronously
        // Wait a bit for async processing
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        verify(deadLetters, never()).push(any());
    }

    @Test
    void testDispatchAskMessage() {
        // Arrange
        AskMessage ask = new AskMessage("agentA", "agentB", "Question?");
        Mailbox mailbox = new Mailbox();
        
        // Act
        dispatcher.dispatch(ask, mailbox);
        
        // Assert - ASK should be logged but not processed asynchronously
        verify(deadLetters, never()).push(any());
    }

    @Test
    void testDispatchWithNullMailbox() {
        // Arrange
        Message msg = new Message("agentA", "agentB", "Test");
        
        // Act
        dispatcher.dispatch(msg, null);
        
        // Assert
        verify(deadLetters, times(1)).push(msg);
    }
}