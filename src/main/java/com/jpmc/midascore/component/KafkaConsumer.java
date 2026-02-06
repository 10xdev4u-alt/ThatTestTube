package com.jpmc.midascore.component;

import com.jpmc.midascore.foundation.Transaction;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class KafkaConsumer {
    
    private final DatabaseConduit databaseConduit;
    private int transactionCount = 0;
    
    public KafkaConsumer(DatabaseConduit databaseConduit) {
        this.databaseConduit = databaseConduit;
    }
    
    @KafkaListener(topics = "${general.kafka-topic}", groupId = "midas-core-group")
    public void listen(Transaction transaction) {
        transactionCount++;
        System.out.println(">>> [" + transactionCount + "] RECEIVED: Amount = " + transaction.getAmount());
        databaseConduit.processTransaction(transaction);
        
        // Print all balances every 5 transactions to see progress
        if (transactionCount % 5 == 0) {
            databaseConduit.printAllUsers();
        }
    }
}
