package featureLocation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithIdentifier;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.printer.YamlPrinter;
import com.github.javaparser.resolution.MethodUsage;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeParameterDeclaration;
import com.github.javaparser.resolution.types.ResolvedPrimitiveType;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.SymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

public class ParseFiles {
	private static String PATH ;
	// final File folder = new File("C:/Users/kev00_000/Desktop/College/4th Year
	// Semester 1/FYP/workspace-jhotdraw ! /JHotDraw7/src/main/java");
	// final File folder = new
	// File(""../JPExamples/src/org/javaparser/samples"");
	// final File folder = new File("C:/workarea/rhino1.7.9/src");

	static HashMap<String, Entity> EntitySet = new HashMap<String, Entity>();
	static ArrayList<Entity> Entities = new ArrayList<Entity>();
	static ArrayList<VariableUsage> VariableUsage = new ArrayList<VariableUsage>();
	private static String root = "";
	private static ArrayList<String> relationships= new ArrayList<String>();
	private int count;

	public boolean parse(String in) throws IOException {

		CompilationUnit cu = null;
		PATH = in + "\\src";
		File baseFile = new File(PATH);
		if (baseFile.exists()) {

			CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
			combinedTypeSolver.add(new ReflectionTypeSolver(false));
			combinedTypeSolver.add(new JavaParserTypeSolver(new File(PATH)));
			JavaParser.getStaticConfiguration().setSymbolResolver(new JavaSymbolSolver(combinedTypeSolver));

			ArrayList<File> filesInFolder = parseDirect(PATH);

			for (int i = 0; i < filesInFolder.size(); i++) {

				System.out.println(i + "/" + filesInFolder.size());
				cu = JavaParser.parse(filesInFolder.get(i));

				String[] split = (filesInFolder.get(i).toString()).split("\\\\");
				Entities.clear();

				root = "";
				for (int k = 0; k < split.length - 1; k++) {
					root += (split[k] + ".");
				}

				// YamlPrinter printer = new YamlPrinter(true);
				// System.out.println(printer.output(cu));

				NodeList<TypeDeclaration<?>> ty = cu.getTypes();
				for (TypeDeclaration<?> typeDeclaration : ty) {
					Node node = (Node) typeDeclaration;
					processNode(node);
				}

				String key;
				for (int e = 0; e < Entities.size(); e++) {
					key = root + Entities.get(e).getName();
					EntitySet.put(key, Entities.get(e));
				}
			}

			matchRelations();

			for (int i = 0; i < filesInFolder.size(); i++) {
				System.out.println(i + "/" + filesInFolder.size());

				String[] split = (filesInFolder.get(i).toString()).split("\\\\");
				root = "";
				for (int k = 0; k < split.length - 1; k++) {
					root += (split[k] + ".");
				}

				cu = JavaParser.parse(filesInFolder.get(i));
				NodeList<TypeDeclaration<?>> ty = cu.getTypes();
				for (TypeDeclaration<?> typeDeclaration : ty) {
					Node node = (Node) typeDeclaration;
					getCalls(node);
				}

			}

			System.out.println(count);
			System.out.println(EntitySet.size());
			// printTest();
			
			if(EntitySet.size()>0){
				return true;
			}
			else
				return false;
			
		} else {
			System.out.println("Invalid Location");
			return false;
		}
	}

	public static ArrayList<File> parseDirect(String in) throws IOException {
		ArrayList<File> filesInFolder = new ArrayList<File>();
		Files.walk(Paths.get(in)).filter(Files::isRegularFile).forEach(filePath -> {
			String name = filePath.getFileName().toString();

			if (name.endsWith(".java")) {
				filesInFolder.add(filePath.toFile());
			}
		});
		;

		return (filesInFolder);

	}

	@SuppressWarnings("deprecation")
	static void processNode(Node node) {
		boolean global = false;

		Entity e = new Entity();

		if (node instanceof ClassOrInterfaceDeclaration) {

			Set<Entity> children = new HashSet<Entity>();

			e.setType(0);
			e.setName(((ClassOrInterfaceDeclaration) node).getNameAsString());

			for (Node child : node.getChildNodes()) {

				Entity c = new Entity();
				if (child instanceof FieldDeclaration) {
					c = getFieldChild(child, e.getName());
				} else {
					c = getClassChild(child, e.getName());
				}
				if (c.getName() != null) {
					c.setParent(e);
					children.add(c);
				}
			}
			e.setChildren(children);
			Entities.add(e);

		}

	}

	static Entity getFieldChild(Node child, String ParentName) {

		Entity e = new Entity();

		for (VariableDeclarator var : ((FieldDeclaration) child).getVariables()) {
			e.setName(ParentName + "." + var.getNameAsString());

			if (child.toString().contains("static")) {
				e.setType(1);
			} else
				e.setType(2);
			Entities.add(e);
		}
		return e;
	}

	static Entity getClassChild(Node node, String ParentName) {

		Entity e = new Entity();

		if (node instanceof MethodDeclaration) {
			Set<Entity> children = new HashSet<Entity>();
			e.setType(3);
			e.setName(ParentName + "." + ((MethodDeclaration) node).getNameAsString());
			children = findVarChild(node, e, children);
			e.setChildren(children);

			Entities.add(e);
			
			MethodDeclaration m = (MethodDeclaration) node;
			m.accept(new VoidVisitorAdapter<Void>() {

				String root2 = PATH.replace("\\", ".");
				@Override
				public void visit(final MethodCallExpr n, final Void arg) {
					
					try{
						relationships.add(root+(m.resolve()).getClassName() + "." + (m.resolve()).getName()+"---"+(root2+"."+n.resolve().getQualifiedName()));
					}catch(Exception e){
						
					}
					
				super.visit(n, arg);
				}
			}, null);
		}

		else if (node instanceof ConstructorDeclaration) {
			e = getConstructor(node, ParentName);
		}

		return e;
	}

	private static Entity getConstructor(Node child, String ParentName) {
		Entity e = new Entity();

		e.setName(ParentName + "." + ((ConstructorDeclaration) child).getNameAsString());
		e.setType(4);
		Entities.add(e);
		return e;
	}

	static Set<Entity> findVarChild(Node node, Entity e, Set<Entity> children) {

		for (Node child : node.getChildNodes()) {

			if (child instanceof VariableDeclarator) {

				Entity c = getMethodChild(child);
				c.setParent(e);
				c.setName(e.getName() + "." + c.getName());
				children.add(c);
				Entities.add(c);
			} else {
				findVarChild(child, e, children);
			}

		}
		return children;
	}

	static Entity getMethodChild(Node node) {

		Entity e = new Entity();
		if (node instanceof VariableDeclarator) {

			e.setType(5);
			e.setName(((VariableDeclarator) node).getNameAsString());
		} else {

			for (Node child : node.getChildNodes()) {
				node = child;
			}
		}

		return e;
	}

	public static void printTest() {
		Iterator it = EntitySet.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pair = (Map.Entry) it.next();
			((Entity) pair.getValue()).print();
		}

		for (int i = 0; i < VariableUsage.size(); i++) {
			VariableUsage.get(i).print();
		}

	}


	public static void getCalls(Node node) {

		for (Node child : node.getChildNodes()) {
			if (child instanceof MethodDeclaration) {
				findVarUsage(child, child.getParentNode());
			}
		}
	}

	static void findVarUsage(Node node, Optional<Node> optional) {

		Optional<Node> Parent = optional;
		for (Node child : node.getChildNodes()) {
			if (child instanceof FieldAccessExpr) {
				getVariableUsage(child, Parent);
			} else {
				findVarUsage(child, Parent);
			}

		}
	}

	private static void getVariableUsage(Node child, Optional<Node> parent) {
		VariableUsage v = new VariableUsage();
		Node p = parent.get();
		
		String callee = root+(child.toString()).substring(0, (child.toString()).indexOf('.'));
		String caller = root+((NodeWithSimpleName<ClassOrInterfaceDeclaration>) p).getNameAsString();
		String variable = root+child.toString();
		
		
		if (EntitySet.get(callee) != null) {
			if (EntitySet.get(caller) != null) {
				if (EntitySet.get(variable) != null) {

					Entity a = EntitySet.get(callee);
					v.setCallee(a);
					
					
					Entity b = EntitySet.get(caller);
					v.setCaller(b);

					
					Entity c = EntitySet.get(variable);
					v.setVariable(c);

					
					VariableUsage.add(v);
					//System.out.println("Variable Usage!");
				}
				
			}
		}

	}
	
	private void matchRelations(){
		try{
			String[]splits;
			Entity f,s;
			
			for(int i=0; i<relationships.size(); i++){
				
				
				splits = relationships.get(i).split("---");
			
				if (EntitySet.get(splits[0]) != null) {
					if (EntitySet.get(splits[1]) != null) {
						f = EntitySet.get(splits[0]);
						s = EntitySet.get(splits[1]);

						f.addIncoming(s);
						s.addOutgoing(f);

						f.setHasIncoming(true);
						s.setHasOutgoing(true);
						EntitySet.put(splits[0], f);
						EntitySet.put(splits[1], s);

						count++;
					}
				}
			}
			}catch(Exception e){
			}
	}

}