package com.example.mobile;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }
}

MessageClient.addListener(MessageClient.OnMessageRecievedListener);
fun onMessageRecieved(messageEvent : MessageEvent) {
    if(messageEvent.path == ) { //put in data type for heart rate after ==
            val startIntent = Intent(this, MainActivity::class.java).apply{
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra("Heart Rate", messageEvent.data)
        }
    } else if(messageEvent.path == ) { //put in data type for heart rate variability after ==
        val startIntent = Intent(this, MainActivity::class.java).apply{
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        putExtra("Heart Rate Variability", messageEvent.data)
        }
    }
}