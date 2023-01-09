package umlparser;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class UMLClassDiagram {
    File inputDir;
    ArrayList<CompilationUnit> javaClassesCUs = new ArrayList<CompilationUnit>();
    String yumlString="";
    Map<String,ClassOrInterfaceDeclaration> classMap = new HashMap<String, ClassOrInterfaceDeclaration>();
    Set<String> dependencies = new HashSet<String>();
    Set<String> associations = new HashSet<String>();

    Map<String,String> makeVarPublic = new HashMap<String,String>();

    boolean debug = Main.DEBUG;

    // Constructor
    public UMLClassDiagram(String path){
        this.inputDir = new File(path);
    }

    // Called from main to parse java code to yuml string
    public String createClassDiagramYUML() throws IOException {
        parseInputDir();
        for(CompilationUnit cu:javaClassesCUs){
            NodeList<TypeDeclaration<?>> classNodes = cu.getTypes();
            for (Node node : classNodes) {
                parseGetterSetter(node);
                // 1. Add class declaration part to yuml string
                addClassDeclaration(node);
                // 2. Add attributes part to yuml
                addAttributes(node);
                // 3. Add methods part to yuml
                addMethods(node);
                // 4. Add links - Inheritance and Implementation links
                addLinks(node);
                System.out.println("\nyUML string:\n"+yumlString);
                System.out.println("\nDependencies:\n"+ String.join(",",dependencies)+"\n");
                System.out.println("\nAssociations:\n"+String.join(",",associations)+"\n");
                //System.out.println("\nClass-CU Map:\n"+classMap);
            }
        }
        return yumlString+String.join(",",dependencies)+","+String.join(",",associations);
    }

    // Creates compilation unit for each class in input directory
    private void parseInputDir() throws IOException {
    for(File file: inputDir.listFiles())
        if(file.getName().endsWith(".java")){

            // Add Javaparser Computational Unit to array
            FileInputStream in = new FileInputStream(file);
            try{
                CompilationUnit classCU= StaticJavaParser.parse(in);
                javaClassesCUs.add(classCU);
                // Map class name and it's parser ComputationUnit
                classMap.put(file.getName().substring(0,file.getName().length()-5),(ClassOrInterfaceDeclaration) classCU.getType(0));
            } finally {
                in.close();
            }
        }
    }

    // Parse getter and setter
    private void  parseGetterSetter(Node node){
        // get Method declarations
        List<MethodDeclaration> methods = ((ClassOrInterfaceDeclaration) node).getMethods();
        for( MethodDeclaration m : methods ){
            String methodName = m.getNameAsString();
            String accessSpecifier = getAccessSpecifierSymbol(m.getAccessSpecifier().asString());
            if(accessSpecifier.equals("+") && (methodName.startsWith("get") || methodName.startsWith("set")) ) {
                String varName = methodName.substring(3).toLowerCase();
                String className = ((ClassOrInterfaceDeclaration) node).getNameAsString();
                makeVarPublic.put(varName,className);
            }
        }
    }


    // Appends class declaration part to yuml string
    private void addClassDeclaration(Node node){
        ClassOrInterfaceDeclaration javaClass = (ClassOrInterfaceDeclaration) node;
        System.out.println("\n\n================ "+javaClass.getName()+" Interface: "+ javaClass.isInterface()+" ================");
        yumlString+= javaClass.isInterface()? "[<<interface>> "+javaClass.getName()+"| " : "["+javaClass.getName()+"| ";
    }

    // Appends class attributes part to yuml string
    private void addAttributes(Node node){
        List<FieldDeclaration> attributes = ((ClassOrInterfaceDeclaration) node).getFields();
        System.out.println("**** Class attribute Declarations ****\n"+attributes);
        String accessSpecifier;
        String type;
        String varName;
        String tempYuml;
        List<VariableDeclarator> variables;

        for(FieldDeclaration a : attributes){
            System.out.println("----------------\n"+"                  Attribute : "+a);
            accessSpecifier = getAccessSpecifierSymbol(a.getAccessSpecifier().asString());
            variables = a.getVariables();

            System.out.println("Variables in the declaration: "+variables+" Size: "+variables.size());
            if(variables.size()>0)
            for(VariableDeclarator v : variables){
                varName=v.getNameAsString();
                type = v.getTypeAsString();
                tempYuml = accessSpecifier+" "+varName+" : "+type;
                System.out.println(tempYuml);

                // Change access specifer of var to Public if it has public getter or setter
                if(makeVarPublic.containsKey(varName)){
                    if(((ClassOrInterfaceDeclaration) node).getNameAsString().equals(makeVarPublic.get(varName)))
                        accessSpecifier="+";
                }

                // Check for Association with other classes
                if( v.getType().isClassOrInterfaceType()){
                    String atribClassName;
                    if(classMap.containsKey(type))
                         atribClassName = getClassName(classMap.get(type));
                    else{
                        if(type.contains("<"))
                            atribClassName = getClassName(classMap.get(type.substring(type.indexOf("<")+1, type.indexOf(">"))));
                        else
                            continue;
                    }

                    String curClassName = getClassName((ClassOrInterfaceDeclaration) node);
                    String multiplicity = getMultiplicity(type);
                    System.out.println("Multiplicity: " +multiplicity);
                    System.out.println("["+atribClassName+"]"+"-"+"["+curClassName+"]");
                    System.out.println();
                    associations.add("["+atribClassName+"]"+multiplicity+varName+"-"+"["+curClassName+"]");
                    continue;
                }

                // If variable is initialised, Get initialised value
                if(v.getInitializer().isPresent()) {
                    tempYuml+= " = "+v.getInitializer().get();
                }
                // If static variable, underline (add formatter "__")
                if(a.isStatic()){
                    tempYuml = "__"+tempYuml+"__";
                }
                // Add yUML variable separator ";"
                tempYuml+=";";
                // Append to final yUML string
                yumlString += cleanSpecialChars(tempYuml);
                System.out.println(tempYuml);
            }
        }
        yumlString = yumlString.substring(0,yumlString.length()-1); // Remove last ";". Otherwise, yUML won't work
        // Add yUML attribute section separator "|"
        yumlString+=" |";
    }

    // Appends class methods part to yuml string
    private void addMethods(Node node){
        String accessSpecifier;
        String returnType;
        String name;
        boolean isAbstract;
        String tempYuml = null;

        // get Constructor declarations
        List<ConstructorDeclaration> constructors = ((ClassOrInterfaceDeclaration) node).getConstructors();
        System.out.println("\n\n******** Constructors ********");
        for(ConstructorDeclaration c : constructors){
            accessSpecifier = getAccessSpecifierSymbol(c.getAccessSpecifier().asString());
            name = c.getNameAsString();

            tempYuml = accessSpecifier+" "+name+"("+getParameters(c)+");";
        }
        if(tempYuml!=null) yumlString += cleanSpecialChars(tempYuml);

        // get Method declarations
        List<MethodDeclaration> methods = ((ClassOrInterfaceDeclaration) node).getMethods();
        System.out.println("\n******** Methods: *******\n"/*+methods*/);

        for(MethodDeclaration m: methods){
            System.out.println(m.getDeclarationAsString());
            returnType = m.getTypeAsString();
            name = m.getNameAsString();
            isAbstract = m.isAbstract();
            accessSpecifier = getAccessSpecifierSymbol(m.getAccessSpecifier().asString());

            tempYuml = accessSpecifier+" "+name+"("+getParameters(m)+") : "+returnType;

            // Skip getters and setters
            if(accessSpecifier.equals("+") && (name.startsWith("get") || name.startsWith("set")) ) continue;


            // If static variable, underline (yUML formatter "__")
            if(m.isStatic()){
                tempYuml = "__"+tempYuml+"__";
            }
            // Add separator ";" before next method
            tempYuml+=";";

            System.out.println(tempYuml);
            // Append to final yUML string
            yumlString += cleanSpecialChars(tempYuml);
        }
        yumlString+="],";
    }

    private void addLinks(Node node){
        ClassOrInterfaceDeclaration classDec = (ClassOrInterfaceDeclaration) node;
        // Inheritance links (Generalization)
        for(ClassOrInterfaceType c : classDec.getExtendedTypes()){
            yumlString+="["+ classDec.getNameAsString()+"]"+"-^"+"["+ c.getNameAsString()+"],";
        }
        // Implementation links
        for(ClassOrInterfaceType i: classDec.getImplementedTypes()){
            yumlString+="["+ classDec.getNameAsString()+"]"+"-.-^"+"[<<interface>> "+ i.getNameAsString()+"],";
        }
    }

    String getAccessSpecifierSymbol(String s){
        switch(s){
            case "PRIVATE": return "-";
            case "PUBLIC" : return "+";
            case "PROTECTED": return "#";
            default: return "+";
        }
    }

    String cleanSpecialChars(String tempYuml){
        // Escape angle brackets; Replace "[" to "［" and "]" to "］"
        tempYuml = tempYuml.replace("<","\\<").replace(">","\\>");
        tempYuml = tempYuml.replace("[","［").replace("]","］");
        return tempYuml;
    }

    String getMultiplicity(String type){
        if(type.matches(".*[<\\[(].*"))
            return "* ";
        else return "";
    }

    String getParameters(MethodDeclaration m){
        String parameters="";
        String paramName, paramType;
        ClassOrInterfaceDeclaration curClassDec = (ClassOrInterfaceDeclaration) m.getParentNode().get();

        // get method parameters
        System.out.println("Parameters: "+m.getParameters());

        for(Parameter p : m.getParameters()){
            paramName = p.getNameAsString();
            paramType = p.getTypeAsString();

            System.out.println("\t"+paramName+" : "+paramType);
            parameters += paramName+" : "+paramType+",";

            // Check if dependency exist with other classes
            checkDependency(curClassDec,paramType);
        }
        if(!parameters.isEmpty())
            parameters = parameters.substring(0,parameters.length()-1);  // To remove last ","

        return parameters;
    }
    String getParameters(ConstructorDeclaration c){
        String parameters="";
        String paramName, paramType;
        ClassOrInterfaceDeclaration curClassDec = (ClassOrInterfaceDeclaration) c.getParentNode().get();

        // get Constructor parameters
        System.out.println("Parameters: "+ c.getParameters());
        for(Parameter p : c.getParameters()){
            paramName = p.getNameAsString();
            paramType = p.getTypeAsString();

            System.out.println("\t"+paramName+" : "+paramType);
            parameters += paramName+" : "+paramType+",";

            // Check if dependency exist with other classes
            checkDependency(curClassDec,paramType);
        }
        if(!parameters.isEmpty())
            parameters = parameters.substring(0,parameters.length()-1);  // To remove last ","

        return parameters;
    }

    // Dependency links
    void checkDependency(ClassOrInterfaceDeclaration curClassDec, String paramClass){
        String curClassName = getClassName((curClassDec));

        if(classMap.containsKey(paramClass) && !curClassDec.isInterface()){
            String paramClassName = getClassName(classMap.get(paramClass));
            //dependencies.add("["+curClassName+"]uses -.->["+paramClassName+"]");
            dependencies.add("["+curClassName+"]-.->["+paramClassName+"]");
        }
    }

    // Class name as per yUML (i.e., with <<interface>> if present)
    String getClassName(ClassOrInterfaceDeclaration classDec){
        return classDec.isInterface()? "<<interface>> "+ classDec.getNameAsString(): classDec.getNameAsString();
    }


}
