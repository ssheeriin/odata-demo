import in.sherinstephen.demo.odata.DemoEdmProvider;
import in.sherinstephen.demo.odata.StudentEntityCollectionProcessor;
import in.sherinstephen.demo.odata.StudentEntityProcessor;
import in.sherinstephen.demo.odata.StudentPrimitiveProcessor;
import in.sherinstephen.demo.service.StudentDAO;
import in.sherinstephen.demo.service.StudentDAOImpl;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataHttpHandler;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.edmx.EdmxReference;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.ArrayList;

/**
 * @author Sherin (I073367)
 * @since 21/2/17
 */
@javax.servlet.annotation.WebServlet(name = "OdataServlet" , urlPatterns = {"/DemoService.svc/*"})
public class OdataServlet extends javax.servlet.http.HttpServlet {

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {

            StudentDAO studentDAO = initializeDataStore(req);

            OData odata = OData.newInstance();
            ServiceMetadata edm = odata.createServiceMetadata(new DemoEdmProvider(), new ArrayList<EdmxReference>());

            ODataHttpHandler handler = odata.createHandler(edm);

            handler.register(new StudentEntityCollectionProcessor(studentDAO));
            handler.register(new StudentEntityProcessor(studentDAO));
            handler.register(new StudentPrimitiveProcessor(studentDAO));

            handler.process(req, resp);
        } catch (RuntimeException e) {
            throw new ServletException("Server Error occurred in OdataServlet", e);
        }
    }

    private StudentDAO initializeDataStore(HttpServletRequest req) {
        HttpSession session = req.getSession(true);
        StudentDAO storage = (StudentDAO) session.getAttribute(StudentDAO.class.getName());
        if (storage == null) {
            storage = new StudentDAOImpl();
            session.setAttribute(StudentDAO.class.getName(), storage);
        }
        return storage;
    }
}
