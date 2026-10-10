
package com.shesuma.shesumamart.listener;

import com.shesuma.shesumamart.util.Database;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@WebListener
public class AppContextListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent event) {
        Database.initialize();
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        Database.shutdown();
    }
}
