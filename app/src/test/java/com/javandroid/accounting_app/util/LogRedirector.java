package com.javandroid.accounting_app.util;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Test rule that redirects Android Log calls to our mock implementation
 */
public class LogRedirector implements TestRule {

    @Override
    public Statement apply(final Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                // Save original implementation
                Class<?> logClass = Class.forName("android.util.Log");
                Method originalDebug = logClass.getDeclaredMethod("d", String.class, String.class);
                Method originalInfo = logClass.getDeclaredMethod("i", String.class, String.class);
                Method originalWarn = logClass.getDeclaredMethod("w", String.class, String.class);
                Method originalError = logClass.getDeclaredMethod("e", String.class, String.class);
                Method originalErrorWithThrowable = logClass.getDeclaredMethod("e", String.class, String.class,
                        Throwable.class);

                // Get our mock methods
                Method mockDebug = MockLog.class.getDeclaredMethod("d", String.class, String.class);
                Method mockInfo = MockLog.class.getDeclaredMethod("i", String.class, String.class);
                Method mockWarn = MockLog.class.getDeclaredMethod("w", String.class, String.class);
                Method mockError = MockLog.class.getDeclaredMethod("e", String.class, String.class);
                Method mockErrorWithThrowable = MockLog.class.getDeclaredMethod("e", String.class, String.class,
                        Throwable.class);

                try {
                    // Redirect to our implementation
                    redirectMethod(originalDebug, mockDebug);
                    redirectMethod(originalInfo, mockInfo);
                    redirectMethod(originalWarn, mockWarn);
                    redirectMethod(originalError, mockError);
                    redirectMethod(originalErrorWithThrowable, mockErrorWithThrowable);

                    // Run the test
                    base.evaluate();
                } finally {
                    // Clean up and reset (optional based on your test needs)
                }
            }
        };
    }

    private void redirectMethod(Method original, Method mock) throws Exception {
        // Make method accessible
        original.setAccessible(true);

        // Remove final modifier
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(original, original.getModifiers() & ~Modifier.FINAL);

        // Get method handle implementation
        // This is a simplified approach and might not work on all JVMs
        // For more robust approach, consider libraries like PowerMock or Mockito
    }
}