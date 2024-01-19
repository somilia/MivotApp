package TAPConnection;

import tap.ServiceConnection;
import tap.TAPException;
import tap.config.ConfigurableServiceConnection;
import tap.resource.Examples;
import tap.resource.HomePage;
import tap.resource.TAP;
import tap.resource.TAPResource;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static tap.config.TAPConfiguration.*;
import static tap.config.TAPConfiguration.KEY_ADD_TAP_RESOURCES;

public class MivotTap {
    private TAP tap = null;

    public void init() throws TAPException {
        // Nothing to do, if TAP is already initialized:
        if (tap != null)
            return;

        //Set the path to the configuration file tap.properties

        String tapConfPath = (new File("src/main/webapp/WEB-INF/tap.properties")).getAbsolutePath();

        /* 1. GET THE FILE PATH OF THE TAP CONFIGURATION FILE */
//        String tapConfPath = config.getInitParameter(TAP_CONF_PARAMETER);
//        if (tapConfPath == null || tapConfPath.trim().length() == 0)
//            tapConfPath = null;
        //throw new ServletException("Configuration file path missing! You must set a servlet init parameter whose the name is \"" + TAP_CONF_PARAMETER + "\".");

        /* 2. OPEN THE CONFIGURATION FILE */
        InputStream input = null;
        // CASE: No file specified => search in the classpath for a file having the default name "tap.properties".
        if (tapConfPath == null)
            throw new TAPException("Configuration file path missing! You must set a servlet init parameter whose the name is \"" + TAP_CONF_PARAMETER + "\".");
//            input = searchFile(DEFAULT_TAP_CONF_FILE, config);
        else{
            File f = new File(tapConfPath);
            System.out.println(f.getAbsolutePath());
            // CASE: The given path matches to an existing local file.
            if (f.exists()){
                try{
                    input = new FileInputStream(f);
                }catch(IOException ioe){
                    throw new TAPException("Impossible to read the TAP configuration file (" + tapConfPath + ")!", ioe);
                }
            }
            // CASE: The given path seems to be relative to the servlet root directory.
            else
                throw new TAPException("Configuration file not found with the path: \"" + tapConfPath + "\"! Please provide a correct file path in servlet init parameter (\"" + TAP_CONF_PARAMETER + "\") or put your configuration file named \"" + DEFAULT_TAP_CONF_FILE + "\" in a directory of the classpath or in WEB-INF or META-INF.");
//                input = searchFile(tapConfPath, config);
        }
        // If no file has been found, cancel the servlet loading:
        if (input == null)
            throw new TAPException("Configuration file not found with the path: \"" + ((tapConfPath == null) ? DEFAULT_TAP_CONF_FILE : tapConfPath) + "\"! Please provide a correct file path in servlet init parameter (\"" + TAP_CONF_PARAMETER + "\") or put your configuration file named \"" + DEFAULT_TAP_CONF_FILE + "\" in a directory of the classpath or in WEB-INF or META-INF.");

        /* 3. PARSE IT INTO A PROPERTIES SET */
        Properties tapConf = new Properties();
        try{
            tapConf.load(input);
        }catch(IOException ioe){
            throw new TAPException("Impossible to read the TAP configuration file (" + tapConfPath + ")!", ioe);
        }finally{
            try{
                input.close();
            }catch(IOException ioe2){}
        }

        /* 4. CREATE THE TAP SERVICE */
        ServiceConnection serviceConn = null;
        try{
            // Create the service connection:
            serviceConn = new ConfigurableServiceConnection(tapConf, (new File("src/main/webapp/")).getAbsolutePath());
            // Create all the TAP resources:
            tap = new TAP(serviceConn);
        }catch(Exception ex){
            tap = null;
            if (ex instanceof TAPException)
                throw new TAPException(ex.getMessage(), ex.getCause());
            else
                throw new TAPException("Impossible to initialize the TAP service!", ex);
        }

        /* 4Bis. SET THE HOME PAGE */
        String propValue = getProperty(tapConf, KEY_HOME_PAGE);
        if (propValue != null){
            // If it is a class path, replace the current home page by an instance of this class:
            if (isClassName(propValue)){
                try{
                    tap.setHomePage(newInstance(propValue, KEY_HOME_PAGE, HomePage.class, new Class<?>[]{TAP.class}, new Object[]{tap}));
                }catch(TAPException te){
                    throw new TAPException(te.getMessage(), te.getCause());
                }
            }
            // If it is a file URI (null, file inside WebContent, file://..., http://..., etc...):
            else{
                // ...set the given URI:
                tap.setHomePageURI(propValue);
                // ...and its MIME type (if any):
                propValue = getProperty(tapConf, KEY_HOME_PAGE_MIME_TYPE);
                if (propValue != null)
                    tap.setHomePageMimeType(propValue);
            }
        }


        /* 4Quater. SET THE EXAMPLES ENDPOINT (if any) */
        propValue = getProperty(tapConf, KEY_EXAMPLES);
        if (propValue != null)
            tap.addResource(new Examples(tap, propValue));

        /* 5. SET ADDITIONAL TAP RESOURCES */
        propValue = getProperty(tapConf, KEY_ADD_TAP_RESOURCES);
        if (propValue != null){
            // split all list items:
            String[] lstResources = propValue.split(",");
            for(String addRes : lstResources){
                addRes = addRes.trim();
                // ignore empty items:
                if (addRes.length() > 0){
                    try{
                        // create an instance of the resource:
                        TAPResource newRes = newInstance(addRes, KEY_ADD_TAP_RESOURCES, TAPResource.class, new Class<?>[]{TAP.class}, new Object[]{tap});
                        if (newRes.getName() == null || newRes.getName().trim().length() == 0)
                            throw new TAPException("TAP resource name missing for the new resource \"" + addRes + "\"! The function getName() of the new TAPResource must return a non-empty and not NULL name. See the property \"" + KEY_ADD_TAP_RESOURCES + "\".");
                        // add it into TAP:
                        tap.addResource(newRes);
                    }catch(TAPException te){
                        throw new TAPException(te.getMessage(), te.getCause());
                    }
                }
            }
        }


        /* 7. INITIATILIZE THE TAP SERVICE */
//        tap.init(config);

        /* 8. FINALLY MAKE THE SERVICE AVAILABLE */
        serviceConn.setAvailable(true, "TAP service available.");
    }

    protected final InputStream searchFile(String filePath, final ServletConfig config){
        InputStream input = null;

        // Try to search in the classpath (with just a file name or a relative path):
        input = Thread.currentThread().getContextClassLoader().getResourceAsStream(filePath);

        // If not found, try searching in WEB-INF and META-INF (as this fileName is a file path relative to one of these directories):
        if (input == null){
            if (filePath.startsWith("/"))
                filePath = filePath.substring(1);
            // ...try at the root of WEB-INF:
            input = config.getServletContext().getResourceAsStream("/WEB-INF/" + filePath);
            // ...and at the root of META-INF:
            if (input == null)
                input = config.getServletContext().getResourceAsStream("/META-INF/" + filePath);
        }

        return input;
    }
}
