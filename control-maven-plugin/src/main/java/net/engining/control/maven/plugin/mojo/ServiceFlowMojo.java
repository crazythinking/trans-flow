package net.engining.control.maven.plugin.mojo;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.ibator.api.GeneratedJavaFile;
import org.apache.ibatis.ibator.api.dom.java.FullyQualifiedJavaType;
import org.apache.ibatis.ibator.api.dom.java.JavaVisibility;
import org.apache.ibatis.ibator.api.dom.java.Method;
import org.apache.ibatis.ibator.api.dom.java.Parameter;
import org.apache.ibatis.ibator.api.dom.java.TopLevelClass;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProject;
import org.sonatype.plexus.build.incremental.BuildContext;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;

import net.engining.control.api.ContextKey;
import net.engining.control.api.InternalKey;
import net.engining.control.api.KeyDefinition;
import net.engining.control.core.flow.FlowDefinition;
import net.engining.control.core.invoker.Invoker;
import net.engining.control.core.invoker.InvokerDefinition;
import net.engining.control.core.invoker.TransactionSeperator;
import net.engining.control.maven.plugin.utils.GeneratorUtils;
import net.engining.control.sdk.AbstractFlowTransPayload;

@Mojo(name = "service-flow", threadSafe = true, defaultPhase = LifecyclePhase.GENERATE_SOURCES, requiresProject = true)
public class ServiceFlowMojo extends AbstractMojo {

	/**
	 * 指定扫描需要生成代码的package
	 */
	@org.apache.maven.plugins.annotations.Parameter(required = true)
	private String scanPackages[];

	/**
	 * 指定生成代码所在的基础package
	 */
	@org.apache.maven.plugins.annotations.Parameter(required = true)
	protected String basePackage;

	/**
	 * 指定生成代码文件的目录
	 */
	@org.apache.maven.plugins.annotations.Parameter(defaultValue = "${project.build.directory}/tc-generated")
	private String outputDirectory;

	/**
	 * 指定生成代码文件的编码
	 */
	@org.apache.maven.plugins.annotations.Parameter(defaultValue = "${project.build.sourceEncoding}")
	private String sourceEncoding;

	@Component
	private BuildContext buildContext;

	@org.apache.maven.plugins.annotations.Parameter(defaultValue = "${project}", readonly = true)
	private MavenProject project;

	/**
	 * 指定生成FlowTrans图的目录
	 */
	@org.apache.maven.plugins.annotations.Parameter(defaultValue = "${project.build.directory}/graphviz-source")
	private String graphvizDirectory;

	/**
	 * 指定生成Request Sample的目录
	 */
	@org.apache.maven.plugins.annotations.Parameter(defaultValue = "${project.build.directory}/request-sample")
	private String requestSampleDirectory;

	/**
	 * ContextKey比较器
	 */
	private final Comparator<Class<? extends ContextKey<?>>> keyComparator = new Comparator<Class<? extends ContextKey<?>>>() {
		public int compare(Class<? extends ContextKey<?>> o1, Class<? extends ContextKey<?>> o2) {
			return o1.getSimpleName().compareTo(o2.getSimpleName());
		}
	};

	public void execute() throws MojoExecutionException, MojoFailureException {
		PrintWriter requestSamplePrinter = null;
		PrintWriter graphvizPrinter = null;
		try {
			ClassLoader classLoader = getClass().getClassLoader();

			Resource r = new Resource();
			r.setDirectory(outputDirectory);
			project.addCompileSourceRoot(outputDirectory);

			// 记录FlowTrans列表
			Properties flowListProperties = new Properties();
			for (String scanPackage : scanPackages) {
				ClassPath classpath = ClassPath.from(classLoader);
				// 扫描出所有顶级类(即除了内部类)
				ImmutableSet<ClassInfo>  classInfos = classpath.getTopLevelClassesRecursive(scanPackage);
				getLog().debug(classInfos.toString());
				for (ClassInfo info : classpath.getTopLevelClassesRecursive(scanPackage)) {
					Class<?> clazz = info.load();

					String relativePackage = StringUtils.remove(clazz.getPackage().getName(), scanPackage);

					FlowDefinition fd = clazz.getAnnotation(FlowDefinition.class);
					// 如果该顶级类不是Flow，则直接执行下一个循环
					if (!Optional.fromNullable(fd).isPresent()) {
						getLog().debug(MessageFormat.format("Class:[{0}]，不是一个FlowTrans，没有@FlowDefinition，已跳过；", clazz.getCanonicalName()));
						continue;
					}

					String flowCode = StringUtils.capitalize(info.getSimpleName());
					getLog().info(MessageFormat.format("开始构建FlowTrans的相关生成文件：[{0}]", flowCode));

					if (flowListProperties.contains(flowCode)) {
						getLog().warn("FlowCode:" + flowCode + "重复，已跳过:" + fd.name());
						continue;
					}
					flowListProperties.put(flowCode, fd.name());

					// 构建Graphviz文件
					getLog().debug("开始构建Graphviz文件：" + flowCode + ".dot");
					File dotFile = new File(graphvizDirectory, flowCode + ".dot");
					FileOutputStream fos = FileUtils.openOutputStream(dotFile);
					OutputStreamWriter osw = new OutputStreamWriter(fos, Charsets.UTF_8);
					graphvizPrinter = new PrintWriter(osw);
					graphvizPrinter.println("digraph g {");
					graphvizPrinter.println("graph [");
					graphvizPrinter.println("rankdir = \"LR\"");
					graphvizPrinter.println(MessageFormat.format("label = \"{0}({1})\"", fd.name(), flowCode));
					graphvizPrinter.println("fontname=\"SimSun\"");
					graphvizPrinter.println("];");

					// FlowDefinition定义的invoker
					Class<? extends Invoker>[] invokers = fd.invokers();
					// 必须输入的上下文属性
					Set<Class<? extends ContextKey<?>>> requires = Sets.newHashSet();
					// 可选输入的上下文属性
					Set<Class<? extends ContextKey<?>>> optional = Sets.newHashSet();

					// 整理FlowTrans输入的上下文属性，包括必须输入的，可选输入的
					// FIXME ?这里逻辑有问题，直接去掉会造成生成graphviz出错，line273；但是将输出项加入确实不合理；
//					getLog().debug("去掉将Flow输出属性，作为必须的上下文属性加入");
					requires.addAll(Arrays.asList(fd.response()));
					for (int i = invokers.length - 1; i >= 0; i--) {
						getLog().debug(MessageFormat.format("处理Flow[{0}], Invoker[{1}]", clazz.getSimpleName(), invokers[i].getSimpleName()));
						InvokerDefinition id = invokers[i].getAnnotation(InvokerDefinition.class);
						checkNotNull(id, "涉及@FlowDefinition定义的Invoker必须使用@InvokerDefinition定义属性");

						requires.removeAll(Arrays.asList(id.results()));
						optional.removeAll(Arrays.asList(id.results()));
						// 再加入各Invoker的必须输入项
						requires.addAll(Arrays.asList(id.requires()));
						// 加入各Invoker的可选输入项
						optional.addAll(Arrays.asList(id.optional()));
					}

					// 生成graphviz内Invoker相关内容，包括输入的上下文属性，输出的上下文属性
					int cluster_count = 0;
					graphvizPrinter.println("subgraph cluster_" + (cluster_count++) + "{");
					graphvizPrinter.println("color=blue;fontname=\"SimSun\";label = \"事务分段 1\";");
					for (Class<? extends Invoker> invoker : invokers) {
						InvokerDefinition id = invoker.getAnnotation(InvokerDefinition.class);
						if (TransactionSeperator.class.equals(invoker)) {
							graphvizPrinter.println("}");
							graphvizPrinter.println("subgraph cluster_" + (cluster_count++) + "{");
							graphvizPrinter.println("color=blue;fontname=\"SimSun\";label = \"事务分段 " + cluster_count + "\";");
							continue;
						}
						StringBuffer sbLabel = new StringBuffer();
						addRecord(sbLabel, "in", id.requires());
						addRecord(sbLabel, "in", id.optional());
						addRecord(sbLabel, "out", id.results());

						createNode(graphvizPrinter, invoker.getSimpleName(), id.name(), sbLabel);

					}

					graphvizPrinter.println("}");
					//排除掉所有必须输入的项，剩下正真可选的项
					optional.removeAll(requires);

					// 生成graphviz内Flow相关内容，包括Request的上下文属性，必输项和可选项；
					Set<Class<? extends ContextKey<?>>> requestRequires = Sets.newHashSet(requires);
					requestRequires.addAll(optional);
					StringBuffer l = new StringBuffer();
					addRecord(l, "out", requestRequires);
					createNode(graphvizPrinter, "_request", "请求对象", l);

					// 生成graphviz内Flow相关内容，包括Response的上下文属性
					l = new StringBuffer();
					addRecord(l, "in", fd.response());
					createNode(graphvizPrinter, "_response", "响应对象", l);

					// 生成graphviz内描述上下文属性的“out->in”对应情况的脚本，对输入项标识“_request:”
					Map<Class<? extends ContextKey<?>>, List<String>> source = Maps.newHashMap();
					for (Class<? extends ContextKey<?>> key : requestRequires) {
						source.put(key, Lists.newArrayList("_request"));
					}

					for (int i = 0; i < invokers.length; i++) {
						InvokerDefinition id = invokers[i].getAnnotation(InvokerDefinition.class);
						for (Class<? extends ContextKey<?>> key : id.requires()) {
							for (String s : source.get(key)) {
								graphvizPrinter.println(MessageFormat.format("{0}:out_{1} -> {2}:in_{1};", s, key.getSimpleName(), invokers[i].getSimpleName()));
							}
						}
						for (Class<? extends ContextKey<?>> key : id.optional()) {
							for (String s : source.get(key)) {
								graphvizPrinter.println(MessageFormat.format("{0}:out_{1} -> {2}:in_{1};", s, key.getSimpleName(), invokers[i].getSimpleName()));
							}
						}
						for (Class<? extends ContextKey<?>> key : id.results()) {
							String invokerName = invokers[i].getSimpleName();

							// 对于Invoker的输出项，如果没有出现在整个FlowTrans的输入项内，不标识“_request:”
							if (!source.containsKey(key)) {
								source.put(key, new ArrayList<String>());
							}

							// 获取接口的泛型对象，如Collection<String>
							ParameterizedType pt = (ParameterizedType) key.getGenericInterfaces()[0];
							// 获取该泛型对象的实际类型，如String
							Type t = pt.getActualTypeArguments()[0];
							if (t instanceof ParameterizedType && (Collection.class.isAssignableFrom((Class<?>) ((ParameterizedType) t).getRawType())
									|| Map.class.isAssignableFrom((Class<?>) ((ParameterizedType) t).getRawType())))

							{
								source.get(key).add(invokerName);
							} else {
								source.get(key).clear();
								source.get(key).add(invokerName);
							}
						}
					}

					// 对于FlowTrans的输出项，标识“_response:”，表明是输出项
					for (Class<? extends ContextKey<?>> key : fd.response()) {
						for (String s : source.get(key)) {
							graphvizPrinter.println(MessageFormat.format("{0}:out_{1} -> {2}:in_{1};", s, key.getSimpleName(), "_response"));
						}
					}
					graphvizPrinter.println("}");
					graphvizPrinter.close();

					// 开始生成FlowTrans对应的Request类
					String requestName = basePackage + relativePackage + "." + flowCode + "Request";
					getLog().debug("构建Request类：" + requestName);
					//FIXME line176 不合理逻辑，这里先打补丁排除
					requires.removeAll(Arrays.asList(fd.response()));
					TopLevelClass request = createWrapper(requestName, fd, flowCode, requires, optional);
					outputClass(request, r);
					
					// 开始生成FlowTrans对应的Response类
					String responseName = basePackage + relativePackage + "." + flowCode + "Response";
					getLog().debug("构建Response类：" + responseName);
					TopLevelClass response = createWrapper(responseName, fd, flowCode, Sets.newHashSet(fd.response()), null);
					outputClass(response, r);

					// 构建Request Sample文件
					getLog().debug("构建Request Sample文件：" + flowCode + ".properties");
					requestSamplePrinter = new PrintWriter(new OutputStreamWriter(
							FileUtils.openOutputStream(new File(requestSampleDirectory, flowCode + ".properties")), Charsets.UTF_8));
					requestSamplePrinter.println("#" + fd.name());
					requestSamplePrinter.println("_request=" + requestName);
					requestSamplePrinter.println("_response=" + responseName);

					requestSamplePrinter.println();
					requestSamplePrinter.println("#必填字段");
					requestSamplePrinter.println();
					List<Class<? extends ContextKey<?>>> sl = Lists.newArrayList(requires);
					Collections.sort(sl, keyComparator);
					for (Class<? extends ContextKey<?>> c : sl) {
						requestSamplePrinter.println("#" + c.getAnnotation(KeyDefinition.class).name());
						requestSamplePrinter.println(StringUtils.uncapitalize(StringUtils.remove(c.getSimpleName(), "Key")) + "=?");
					}

					requestSamplePrinter.println();
					requestSamplePrinter.println("#选填字段");
					requestSamplePrinter.println();
					sl = Lists.newArrayList(optional);
					Collections.sort(sl, keyComparator);
					for (Class<? extends ContextKey<?>> c : sl) {
						requestSamplePrinter.println("#" + c.getAnnotation(KeyDefinition.class).name());
						requestSamplePrinter.println(StringUtils.uncapitalize(StringUtils.remove(c.getSimpleName(), "Key")) + "=");
					}
					requestSamplePrinter.close();
					
					getLog().info(MessageFormat.format("结束构建FlowTrans的相关生成文件：[{0}]", flowCode));
				}

			}

			//生成FlowTrans的汇总文件
			Resource listResource = new Resource();
			listResource.setDirectory(outputDirectory);
			String listFile = project.getArtifactId() + ".properties";
			PrintWriter flowtransSummaryPrinter = new PrintWriter(new OutputStreamWriter(
					FileUtils.openOutputStream(new File(outputDirectory, listFile)), Charsets.UTF_8));
			//将FlowTrans列表写入Properties文件
			flowListProperties.store(flowtransSummaryPrinter, "Flow Transactions List");
			flowtransSummaryPrinter.close();
			
			listResource.addInclude(listFile);
			project.addResource(listResource);
			
		} catch (Exception e) {
			e.printStackTrace();
			throw new MojoFailureException("执行失败", e);
			
		} finally {
			IOUtils.closeQuietly(graphvizPrinter);
			IOUtils.closeQuietly(requestSamplePrinter);
			
		}
		
	}

	/**
	 * 生成graphviz内label = "xxx |后的内容，主要包括in和out的属性
	 * 
	 * @param label
	 * @param prefix
	 * @param props
	 */
	private void addRecord(StringBuffer label, String prefix, Class<? extends ContextKey<?>> props[]) {
		addRecord(label, prefix, Arrays.asList(props));
	}

	/**
	 * 生成graphviz内label = "xxx |后的内容，主要包括in和out的属性
	 * 
	 * @param label
	 * @param prefix
	 * @param props
	 */
	private void addRecord(StringBuffer label, String prefix, Collection<Class<? extends ContextKey<?>>> props) {
		List<Class<? extends ContextKey<?>>> list = Lists.newArrayList(props);
		Collections.sort(list, new Comparator<Class<? extends ContextKey<?>>>() {

			public int compare(Class<? extends ContextKey<?>> o1, Class<? extends ContextKey<?>> o2) {
				return o1.getSimpleName().compareTo(o2.getSimpleName());
			}
		});
		for (Class<? extends ContextKey<?>> key : list) {
			if (label.length() > 0)
				label.append("|");
			label.append(MessageFormat.format("<{0}_{1}> {2}", prefix, key.getSimpleName(), StringUtils.remove(key.getSimpleName(), "Key")));
		}
	}

	/**
	 * 生成graphviz内Node的内容，将addRecord中根据Invoker定义生成的内容插入
	 * 
	 * @param pw
	 * @param name
	 * @param title
	 * @param label
	 */
	private void createNode(PrintWriter pw, String name, String title, StringBuffer label) {
		pw.println(MessageFormat.format("\"{0}\" [\n fontname=\"SimSun\" \n label = \" {0}({1}) | {2}\"\nshape=\"Mrecord\"\n];", name, title, label));
	}

	/**
	 * 生成Request和Response类源文件，分别加入编译上下文（BuildContext），和源文件上下文（Resource）
	 * @param clazz
	 * @param resource
	 * @throws IOException
	 */
	private void outputClass(TopLevelClass clazz, Resource resource) throws IOException {
		GeneratedJavaFile gjf = new GeneratedJavaFile(clazz, ".");
		String filename = MessageFormat.format("{0}/{1}", StringUtils.replace(gjf.getTargetPackage(), ".", "/"), gjf.getFileName());
		File targetFile = new File(FilenameUtils.concat(outputDirectory, filename));
		getLog().info("生成文件:" + targetFile.getCanonicalPath());

		FileUtils.writeStringToFile(targetFile, clazz.getFormattedContent(), sourceEncoding);
		buildContext.refresh(targetFile);
		resource.addInclude(filename);
	}

	/**
	 * 用于生成Request和Response类
	 * @param className
	 * @param fd
	 * @param flowCode
	 * @param requires
	 * @param optional
	 * @return
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	private TopLevelClass createWrapper(String className, FlowDefinition fd, String flowCode, Set<Class<? extends ContextKey<?>>> requires,
			Set<Class<? extends ContextKey<?>>> optional) throws InstantiationException, IllegalAccessException {
		TopLevelClass clazz = new TopLevelClass(new FullyQualifiedJavaType(className));
		
		//public class
		clazz.setVisibility(JavaVisibility.PUBLIC);
		//imorpt AbstractFlowTransPayload
		FullyQualifiedJavaType fqjtRequestSuper = new FullyQualifiedJavaType(AbstractFlowTransPayload.class.getCanonicalName());
		clazz.addImportedType(fqjtRequestSuper);
		//extends AbstractFlowTransPayload
		clazz.setSuperClass(fqjtRequestSuper);
		
		//java doc
		clazz.addJavaDocLine("/**");
		clazz.addJavaDocLine(" * " + fd.name());
		if (StringUtils.isNotEmpty(fd.desc())) {
			clazz.addJavaDocLine(" * " + fd.desc());
		}
		clazz.addJavaDocLine(" */");
		
		//构造函数
		Method cons = new Method();
		cons.setVisibility(JavaVisibility.PUBLIC);
		cons.setName(clazz.getType().getShortName());
		cons.setConstructor(true);
		cons.addBodyLine(MessageFormat.format("super(\"{0}\");", flowCode));
		clazz.addMethod(cons);
		
		//类属性
		Set<Class<? extends ContextKey<?>>> all = Sets.newHashSet();
		all.addAll(requires);
		if (optional != null) {
			all.addAll(optional);
		}
		//为所有属性添加方法
		for (Class<? extends ContextKey<?>> key : all) {
			if (key.getAnnotation(InternalKey.class) != null) {
				// 内部使用，不作为外部接口
				continue;
			}

			ParameterizedType t = (ParameterizedType) key.getGenericInterfaces()[0];
			//imorpt 属性value对应的类
			FullyQualifiedJavaType fqjtValue = createFromTypeAndImport(clazz, t.getActualTypeArguments()[0], key.getCanonicalName());
			FullyQualifiedJavaType fqjtKey = new FullyQualifiedJavaType(key.getCanonicalName());

			//import 属性key对应的类
			clazz.addImportedType(fqjtKey);

			//为属性生成getter和setter方法
			String propertyName = StringUtils.uncapitalize(StringUtils.remove(key.getSimpleName(), "Key"));
			KeyDefinition kd = key.getAnnotation(KeyDefinition.class);
			String keyName;
			if (kd == null) {
				keyName = "";
				getLog().warn(MessageFormat.format("{0} 没有指定 @KeyDefinition", key.getName()));
			} else {
				keyName = kd.name();
			}
			//getter
			Method method = new Method();
			method.setVisibility(JavaVisibility.PUBLIC);
			method.setReturnType(fqjtValue);
			method.setName(GeneratorUtils.getGetterMethodName(propertyName, fqjtValue));
			method.addBodyLine(MessageFormat.format("return ({0})dataMap.get({1}.class);", fqjtValue.getShortName(), fqjtKey.getShortName()));
			method.addJavaDocLine("/*");
			method.addJavaDocLine(" * " + keyName);
			method.addJavaDocLine(" */");
			clazz.addMethod(method);
			//setter
			method = new Method();
			method.setVisibility(JavaVisibility.PUBLIC);
			method.addParameter(new Parameter(fqjtValue, "value"));
			method.setName(GeneratorUtils.getSetterMethodName(propertyName));
			method.addBodyLine(MessageFormat.format("dataMap.put({0}.class, value);", fqjtKey.getShortName()));
			method.addJavaDocLine("/*");
			method.addJavaDocLine(" * " + keyName);
			method.addJavaDocLine(" */");
			clazz.addMethod(method);
		}
		return clazz;
	}

	/**
	 * 根据Type，创建import
	 * @param clazz
	 * @param type
	 * @param name
	 * @return
	 */
	private FullyQualifiedJavaType createFromTypeAndImport(TopLevelClass clazz, Type type, String name) {
		if (type instanceof Class) {
			Class<?> c = (Class<?>) type;
			FullyQualifiedJavaType fqjt = new FullyQualifiedJavaType(c.getCanonicalName());
			if (c.isArray()) {
				// 数组的话取元素类型
				clazz.addImportedType(new FullyQualifiedJavaType(c.getComponentType().getCanonicalName()));
			} else {
				clazz.addImportedType(fqjt);
			}
			return fqjt;
		} else if (type instanceof GenericArrayType) {
			// JDK6会对数组返回这个
			GenericArrayType gat = (GenericArrayType) type;
			FullyQualifiedJavaType fqjt = new FullyQualifiedJavaType(gat.toString());
			createFromTypeAndImport(clazz, gat.getGenericComponentType(), name);
			return fqjt;
		} else if (type instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType) type;
			FullyQualifiedJavaType fqjt = new FullyQualifiedJavaType(((Class<?>) pt.getRawType()).getCanonicalName());
			Type ts[] = pt.getActualTypeArguments();
			for (Type t : ts) {
				fqjt.addTypeArgument(createFromTypeAndImport(clazz, t, name));
			}

			clazz.addImportedType(fqjt);
			return fqjt;
		} else {
			throw new IllegalArgumentException(
					"should not be here:" + type.getClass().getCanonicalName() + " / " + clazz.getType().getFullyQualifiedName() + "/" + name);
		}
	}

	public static interface K extends ContextKey<byte[]> {
	}

	public static void main(String[] args) {
		Class<? extends ContextKey<?>> clazz = K.class;
		ParameterizedType pt = (ParameterizedType) clazz.getGenericInterfaces()[0];
		Type t = pt.getActualTypeArguments()[0];
		if (t instanceof ParameterizedType) {
			ParameterizedType ppt = (ParameterizedType) t;
			Class<?> s = (Class<?>) ppt.getRawType();
			System.out.println(Collection.class.isAssignableFrom(s));
		}
		// byte [] a = new byte[2];
		// System.out.println(a.getClass().getComponentType());
	}
}
