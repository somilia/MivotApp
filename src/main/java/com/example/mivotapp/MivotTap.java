package com.example.mivotapp;
import ModelBaseInit.ModelBaseInit;

import static tap.config.TAPConfiguration.newInstance;

import tap.TAPException;
import tap.resource.TAP;

import java.io.*;
import java.util.ArrayList;

import jakarta.servlet.annotation.*;
import tap.config.ConfigurableTAPServlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(name = "mivotTap", value = "/mivot-tap")
public class MivotTap extends ConfigurableTAPServlet {
    private String message;
    private TAP tap;
    @Override
    public void init() throws ServletException {
        // Override the init method of the parent class to initialize the TAP service:
        super.init();
        ArrayList<String> col_to_query = new ArrayList<>();
        col_to_query.add("sc_ra");
        col_to_query.add("sc_pm_ra");
        col_to_query.add("sc_dec");
        col_to_query.add("sc_err_min");
        try {
            ModelBaseInit ModelbaseInit = new ModelBaseInit();
            ModelbaseInit.getModelBase("epic_src","mango", col_to_query);
        } catch (TAPException e) {
            throw new RuntimeException(e);
        }

    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html");

        // Hello
        PrintWriter out = response.getWriter();
        out.println("<html><body>");
        out.println("<h1>" + message + "</h1>");
        out.println("</body></html>");
    }

    public void destroy() {
    }
}