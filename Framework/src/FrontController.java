package mg.itu.prom16;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.*;
import javax.servlet.http.*;

public class FrontController extends HttpServlet{
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
        try {
            processRequest(request, response);    
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
        try {
            processRequest(request, response);    
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void processRequest(HttpServletRequest request, HttpServletResponse response) throws Exception{
        try {
            response.setContentType("text/html");

            PrintWriter out = response.getWriter();
            out.println("<html><head><title>FrontController</title></head><body>");
            out.println(request.getRequestURL());
            out.println("</body></html>");   
        } catch (Exception e) {
            throw e;
        }
    }
}