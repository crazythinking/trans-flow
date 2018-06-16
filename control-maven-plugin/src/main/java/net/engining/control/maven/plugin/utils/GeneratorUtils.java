package net.engining.control.maven.plugin.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.apache.ibatis.ibator.api.dom.java.CompilationUnit;
import org.apache.ibatis.ibator.api.dom.java.Field;
import org.apache.ibatis.ibator.api.dom.java.FullyQualifiedJavaType;
import org.apache.ibatis.ibator.api.dom.java.JavaVisibility;
import org.apache.ibatis.ibator.api.dom.java.Method;
import org.apache.ibatis.ibator.api.dom.java.Parameter;
import org.apache.ibatis.ibator.api.dom.java.TopLevelClass;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

public class GeneratorUtils
{
	public static String dbName2ClassName(String dbName)
	{
		String s = dbName;
		
		boolean allUpperCaseOrNumeric = true;
		for (char c : s.toCharArray())
		{
			if (c != '_' && !CharUtils.isAsciiNumeric(c) && !CharUtils.isAsciiAlphaUpper(c))
			{
				allUpperCaseOrNumeric = false;
				break;
			}
		}
		
		if (allUpperCaseOrNumeric)
		{
			//为应对Java类定义的情况，只有在全大写时才需要定义
			//TODO 这是临时方案
			s = s.toLowerCase();
			s = WordUtils.capitalizeFully(s, new char[]{ '_' });
			s = StringUtils.remove(s, "_");
		}
		
		if (!StringUtils.isAlpha(StringUtils.left(s, 1)))	//避免首个不是字母的情况
			s = "_" + s;
		return s;
	}

	public static String dbName2PropertyName(String dbName)
	{
		return WordUtils.uncapitalize(dbName2ClassName(dbName));
	}

	public static FullyQualifiedJavaType forType(TopLevelClass topLevelClass, String type)
	{
		FullyQualifiedJavaType fqjt = new FullyQualifiedJavaType(type);
		topLevelClass.addImportedType(fqjt);
		return fqjt;
	}

	public static Field generateProperty(TopLevelClass clazz, FullyQualifiedJavaType fqjt, String property, List<String> javadoc, boolean trimStrings)
	{
		clazz.addImportedType(fqjt);

		Field field = new Field();
		field.setVisibility(JavaVisibility.PRIVATE);
		field.setType(fqjt);
		field.setName(property);
		
		clazz.addField(field);
		
		//getter
		Method method = new Method();
		method.setVisibility(JavaVisibility.PUBLIC);
		method.setReturnType(fqjt);
		method.setName(getGetterMethodName(field.getName(), field.getType()));
		StringBuilder sb = new StringBuilder();
		sb.append("return ");
		sb.append(property);
		sb.append(';');
		method.addBodyLine(sb.toString());

		createJavadoc(method, javadoc);

		clazz.addMethod(method);

		//setter
		method = new Method();
		method.setVisibility(JavaVisibility.PUBLIC);
		method.setName(getSetterMethodName(property));
		method.addParameter(new Parameter(fqjt, property));
		createJavadoc(method, javadoc);

		if (trimStrings && fqjt.equals(FullyQualifiedJavaType.getStringInstance()))
		{
			sb.setLength(0);
			sb.append("this."); //$NON-NLS-1$
			sb.append(property);
			sb.append(" = "); //$NON-NLS-1$
			sb.append(property);
			sb.append(" == null ? null : "); //$NON-NLS-1$
			sb.append(property);
			sb.append(".trim();"); //$NON-NLS-1$
			method.addBodyLine(sb.toString());
		}
		else
		{
			sb.setLength(0);
			sb.append("this."); //$NON-NLS-1$
			sb.append(property);
			sb.append(" = "); //$NON-NLS-1$
			sb.append(property);
			sb.append(';');
			method.addBodyLine(sb.toString());
		}

		clazz.addMethod(method);

		return field;
	}

	private static void createJavadoc(Method method, List<String> javadoc) {
		if (javadoc != null)
		{
			method.addJavaDocLine("/**");
			for (String line : javadoc)
			{
				method.addJavaDocLine(" * <p>" + line + "</p>");
			}
			method.addJavaDocLine(" */");
		}
	}
	
	public static String getGetterMethodName(String property, FullyQualifiedJavaType fullyQualifiedJavaType)
	{
		String name = StringUtils.capitalize(property);

		if (fullyQualifiedJavaType.equals(FullyQualifiedJavaType.getBooleanPrimitiveInstance()))
			name = "is" + name;
		else
			name = "get" + name;
		return name;
	}

    public static String getSetterMethodName(String property)
    {
		String name = StringUtils.capitalize(property);
		return "set" + name;
    }
    
    public static File createGwtModule(String outputDirectory, String basePackage, String moduleName, Collection<CompilationUnit> units) throws IOException
    {
		Set<String> sources = new HashSet<String>();
		for (CompilationUnit unit : units)
		{
			String source = StringUtils.remove(unit.getType().getPackageName(), basePackage).substring(1).replace('.', '/');
			sources.add(source);
		}
		Document doc = DocumentHelper.createDocument();
		Element root = DocumentHelper.createElement("module");
		doc.add(root);
		doc.addDocType("module", "-//Google Inc.//DTD Google Web Toolkit 2.5.1//EN", "http://google-web-toolkit.googlecode.com/svn/tags/2.5.1/distro-source/core/src/gwt-module.dtd");
		
		for (String source : sources)
			root.addElement("source").addAttribute("path", source);
		
		
		File moduleFile = new File(outputDirectory, basePackage.replace('.', '/')+"/" + moduleName + ".gwt.xml");
		FileOutputStream fos = null;
		try
		{
			fos = new FileOutputStream(moduleFile);
			XMLWriter xw = new XMLWriter(fos, OutputFormat.createPrettyPrint());
			xw.write(doc);
			xw.close();
		}
		finally
		{
			IOUtils.closeQuietly(fos);
		}
		return moduleFile;
    }
    
}
