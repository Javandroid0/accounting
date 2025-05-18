package com.javandroid.accounting_app.data.repository;

import android.app.Application;

/**
 * Manager class that provides access to OrderStateRepository instances.
 * This class is the central point for getting repositories and ensures
 * that ViewModels can share the same repository instance when needed.
 */
public class OrderSessionManager {
    private static OrderSessionManager instance;

    // The current active repository for new orders
    private OrderStateRepository currentRepository;

    /**
     * Private constructor for singleton pattern
     */
    private OrderSessionManager() {
        // Create an initial repository
        createNewSession(0);
    }

    /**
     * Get the singleton instance of the manager
     */
    public static synchronized OrderSessionManager getInstance() {
        if (instance == null) {
            instance = new OrderSessionManager();
        }
        return instance;
    }

    /**
     * Get the current active repository
     */
    public OrderStateRepository getCurrentRepository() {
        return currentRepository;
    }

    /**
     * Create a new session with a new repository
     * 
     * @param userId The user ID to associate with the new session
     * @return The newly created repository
     */
    public OrderStateRepository createNewSession(long userId) {
        currentRepository = new OrderStateRepository(userId);
        return currentRepository;
    }

    /**
     * Reset the current repository to a clean state
     * 
     * @param userId The user ID to associate with the reset repository
     */
    public void resetCurrentSession(long userId) {
        if (currentRepository != null) {
            currentRepository.reset(userId);
        } else {
            createNewSession(userId);
        }
    }
}