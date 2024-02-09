package MangoMivotBuild;

import tap.config.ConfigurableTAPServlet;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

public class MivotTAPServlet extends ConfigurableTAPServlet {
    /**
     * This class is used to get the servlet context in order to retrieve our mivot_snippets path.
     * The Servlet class has been added to the web.xml
     */
    public static ServletContext servletContext;

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        servletContext = config.getServletContext();
    }
}
