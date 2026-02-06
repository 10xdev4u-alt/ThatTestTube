package com.jpmc.midascore.component;

import com.jpmc.midascore.entity.TransactionRecord;
import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.foundation.Transaction;
import com.jpmc.midascore.repository.TransactionRecordRepository;
import com.jpmc.midascore.repository.UserRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class DatabaseConduit {
    
    private final UserRepository userRepository;
    private final TransactionRecordRepository transactionRecordRepository;
    
    public DatabaseConduit(UserRepository userRepository, 
                          TransactionRecordRepository transactionRecordRepository) {
        this.userRepository = userRepository;
        this.transactionRecordRepository = transactionRecordRepository;
    }
    
    public void save(UserRecord user) {
        userRepository.save(user);
    }
    
    @Transactional
    public void processTransaction(Transaction transaction) {
        // Find sender and recipient
        Optional<UserRecord> senderOpt = userRepository.findById(transaction.getSenderId());
        Optional<UserRecord> recipientOpt = userRepository.findById(transaction.getRecipientId());
        
        // Validate: both users must exist
        if (senderOpt.isEmpty() || recipientOpt.isEmpty()) {
            System.out.println(">>> INVALID: User not found");
            return;
        }
        
        UserRecord sender = senderOpt.get();
        UserRecord recipient = recipientOpt.get();
        
        // Validate: sender must have sufficient balance
        if (sender.getBalance() < transaction.getAmount()) {
            System.out.println(">>> INVALID: Insufficient balance for " + sender.getName());
            return;
        }
        
        // Process valid transaction
        sender.setBalance(sender.getBalance() - transaction.getAmount());
        recipient.setBalance(recipient.getBalance() + transaction.getAmount());
        
        // Save updated balances
        userRepository.save(sender);
        userRepository.save(recipient);
        
        // Record transaction
        TransactionRecord record = new TransactionRecord(sender, recipient, transaction.getAmount());
        transactionRecordRepository.save(record);
        
        System.out.println(">>> PROCESSED: " + sender.getName() + " -> " + recipient.getName() 
            + " | Amount: " + transaction.getAmount());
        
        // Print waldorf's balance if involved
        if ("waldorf".equalsIgnoreCase(sender.getName()) || "waldorf".equalsIgnoreCase(recipient.getName())) {
            Optional<UserRecord> waldorf = findUserByName("waldorf");
            waldorf.ifPresent(w -> System.out.println(">>> WALDORF BALANCE: " + w.getBalance()));
        }
    }
    
    public Optional<UserRecord> findUserById(long id) {
        return userRepository.findById(id);
    }
    
    public Optional<UserRecord> findUserByName(String name) {
        List<UserRecord> users = (List<UserRecord>) userRepository.findAll();
        return users.stream()
            .filter(u -> name.equalsIgnoreCase(u.getName()))
            .findFirst();
    }
    
    public void printAllUsers() {
        System.out.println(">>> ALL USERS:");
        userRepository.findAll().forEach(u -> 
            System.out.println("    " + u.getName() + ": " + u.getBalance())
        );
    }
}
