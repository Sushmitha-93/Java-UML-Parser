/* (c) Copyright 2020 Paul Nguyen. All Rights Reserved */

package umlparser ;

import java.io.*;

/*
    Apache HTTP Client 3.0 (Legacy)

    http://hc.apache.org/httpclient-legacy/index.html
    https://hc.apache.org/httpclient-3.x/apidocs/
*/

import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.*;

/**
 * Main Entry Point.
 */
final class Main {

    static boolean DEBUG = true ;

    /**
     * Prevent Construction.
     */
    private Main() {
        // Utility Class
        return ;
    }

    /**
     * Create Dummy File
     * @param dir directory location for file
     * @param file file to create (.ext included)
     */
    private static void touch ( String dir, String filename ) {
        File file = new File( dir, filename ) ;
        if ( !file.exists() ) {
            try {
                file.createNewFile()  ;
            } catch ( Exception e ) {
                System.out.println( e ) ;
            }
        }
    }

    /**
     * [copyInputStreamToFile description]
     * @param  inputStream [description]
     * @param  filename    [description]
     * @throws IOException [description]
     */
    private static void copyInputStreamToFile(InputStream inputStream, String filename)
    throws IOException {

        File file = new File( filename );
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            int read;
            byte[] bytes = new byte[1024];
            while ((read = inputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, read);
            }
        }

    }


    /**
     * Main App Entry Point.
     * @param args base-dir test-case
     */
    public static void main(final String[] args) throws IOException {

        if ( DEBUG ) {
            System.out.printf( "[DEBUG] UML Parser #Args: %d%n", args.length ) ;
            for (int i = 0; i < args.length; i++)  {
                System.out.printf( "[DEBUG] args[%d] = %s%n", i, args[i] );
            }
        }
        if ( args.length == 2 ) {

            String basedir = args[0] ;
            String testcase = args[1] ;
            String testdir = basedir + File.separator + "testcases" + File.separator + testcase ;
            String outdir = basedir + File.separator + "output" + File.separator + testcase ;
            if ( DEBUG ) {
                System.out.printf("basedir: %s%n", basedir) ;
                System.out.printf("testdir: %s%n", testdir) ;
                System.out.printf("outdir: %s%n", outdir) ;                
            }

            if ( testcase.equals("sequence") ) {
                touch(outdir, testcase + ".seq") ;  // sequence source
                touch(outdir, testcase + ".jpg") ;  // sequence output
            } else {
                UMLClassDiagram classDiagram = new UMLClassDiagram(testdir);
                String yumlString = classDiagram.createClassDiagramYUML();
                YUML uml = new YUML( basedir ) ;
                String image = uml.generate( yumlString, testcase ) ;
                System.out.println("Generated: " + image) ;

                touch(outdir, testcase + ".yuml") ; // yuml source
                touch(outdir, testcase + ".jpg") ;  // yuml output
            }
        }
        else {
            System.out.println( "java -cp [jar file].jar umlparser.Main [base directory] [test case]" ) ;
        }

    }

}

