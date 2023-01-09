package umlparser;

import java.io.*;
import java.util.*;
import java.lang.*;

import com.github.javaparser.*;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.TypeParameter;


public class test {

    public static void main(final String[] args) throws IOException {

        String testDir= "D:\\Fall 22 Courses\\202 - SW Systems Engg\\cmpe202-sushmitha-93-main\\umlparser\\testcases\\test1";
        System.out.println("testDir: "+testDir+"\n");

        File folder = new File(testDir);
        for(File f:folder.listFiles())
            System.out.println(f);

        FileInputStream in = new FileInputStream("D:\\Fall 22 Courses\\202 - SW Systems Engg\\cmpe202-sushmitha-93-main\\umlparser\\testcases\\test1\\A.java");

        CompilationUnit cu ;
        JavaParser jp = new JavaParser();

            cu = StaticJavaParser.parse(in);

            in.close();


        // prints the resulting compilation unit to default system output


        NodeList<TypeDeclaration<?>> classNodes = cu.getTypes();


        for (Node node : classNodes) {
            ClassOrInterfaceDeclaration classOrInterface = (ClassOrInterfaceDeclaration) node;
            System.out.println(classOrInterface.getName());
            System.out.println("Interface: "+ classOrInterface.isInterface());
            System.out.println(classOrInterface.isAbstract());
            System.out.println(classOrInterface.getExtendedTypes());


            List<MethodDeclaration> methods = ((ClassOrInterfaceDeclaration) node).getMethods();
            System.out.println("\n\n**** Methods: ****\n"+ methods);
            for(MethodDeclaration m : methods){
                System.out.println(m.getDeclarationAsString());
                System.out.println(m.getAccessSpecifier()); // PRIVATE, PUBLIC, PROTECTED, NONE
                System.out.println(m.getType());
                System.out.println(m.getSignature());
                System.out.println(m.isAbstract());
                System.out.println(m.isStatic());
                System.out.println("Parameters: "+m.getParameters());
                for(Parameter p : m.getParameters()){
                    System.out.println("\t"+p.getNameAsString()+" : "+p.getTypeAsString());
                    // To get current class name
                    System.out.println(((ClassOrInterfaceDeclaration) (p.getParentNode().get().getParentNode().get())).getNameAsString());
                }
                // To get class name of method declared in
                System.out.println( ((ClassOrInterfaceDeclaration) m.getParentNode().get()).getName());

            }

            List<ConstructorDeclaration> constructors = ((ClassOrInterfaceDeclaration) node).getConstructors();
            System.out.println("\n\n ******** Constructors ********");
            for(ConstructorDeclaration c : constructors){
                System.out.println(c.getDeclarationAsString());
                System.out.println("\t"+c.getAccessSpecifier().asString());
                System.out.println("\t"+c.getSignature());
                for(Parameter p : c.getParameters()){
                    System.out.println("\t"+p.getNameAsString()+" : "+p.getTypeAsString());
                }
            }

            List<FieldDeclaration> ad = ((ClassOrInterfaceDeclaration) node).getFields();
            System.out.println("\n\n ********* Field Declarations *********\n");
            System.out.println(ad);
            for(FieldDeclaration a : ad){
                System.out.println("----------------");
                System.out.println("Static: "+a.isStatic());
                System.out.println(("Final: "+a.isFinal()));
                System.out.println(a);
                System.out.println(a.getAccessSpecifier());
                System.out.println(a.getElementType());
                System.out.println(a.getVariables());
                for(VariableDeclarator v: a.getVariables()){
                    System.out.println("-var-");
                    System.out.println("\t"+v.getName());
                    System.out.println("\t"+v.getTypeAsString());
                    System.out.println(v.getType().isClassOrInterfaceType());
                    System.out.println(v.getType().isArrayType());

                    if(v.getInitializer().isPresent())
                        System.out.println(v.getInitializer().get());
                }
                System.out.println();
               // System.out.println(a.setModifiers());


            }




        }
        System.out.println("Collection<B>".matches(".*[<\\[(].*"));
        System.out.println("Collection[]".matches(".*[<\\[(].*"));
        System.out.println("Collection".matches(".*[<\\[(].*"));
        String str="Collection<B>";
        System.out.println(str.substring(str.indexOf("<")+1, str.indexOf(">")));




    }
}
