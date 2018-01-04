import org.jsoup.Jsoup;           //Jsoup
import org.jsoup.nodes.Document;  //document
import org.jsoup.nodes.Element;   //element
import org.jsoup.select.Elements; //elements
import java.io.File;              //文件
import java.io.IOException;       //IOException
import java.io.PrintWriter;       //文件
import java.sql.Connection;       //数据库链接
import java.sql.Date;             //Date类型,日期
import java.sql.DriverManager;    //Driver驱动
import java.sql.SQLException;     //SQL异常
import java.sql.PreparedStatement;//PreparedStatement类
import java.util.LinkedHashSet;   //Set
import java.util.Set;             //set集合
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Main
{
    public static Document Get(String url) throws IOException  //得到网页源码
    {
        Document document = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/62.0.3202.89 Safari/537.36")
                .get();
        return document;
    }

    public static Connection connection = null; // 数据库连接

    static void ConnectDatabase() throws SQLException,ClassNotFoundException
    {
        String databaseURL = "jdbc:mysql://localhost:3306/dangdangnet";
        String user = "root";
        String password = "gmr19980305";
        String driver = "com.mysql.jdbc.Driver";
        try
        {
            Class.forName(driver); //加载一个JDBC驱动
            connection = DriverManager.getConnection(databaseURL, user, password);
            if(!connection.isClosed())
                System.out.println("Succeeded connecting to the Database!");

        } catch(ClassNotFoundException e) {
            //数据库驱动类异常处理
            System.out.println("Sorry,can`t find the Driver!");
            e.printStackTrace();

        } catch(SQLException e1) {
            //数据库连接失败异常处
            e1.printStackTrace();

        }catch (Exception e2) {
            // TODO: handle exception
            e2.printStackTrace();

        }finally{
            System.out.println("数据库数据成功获取！！");
        }
    }

    public static void main(String[] args) throws IOException, SQLException, ClassNotFoundException, IllegalArgumentException
    {
        ConnectDatabase(); //数据库链接

        //得到当当网计算机首页的全部可以点开的链接
        Document docoumentAll = Get("http://book.dangdang.com/01.54.htm");
        Element elementAll = docoumentAll.select(".flq_body").get(0);
        Elements elementsAll = elementAll.getElementsByTag("a");

        Set<String> urlAll = new LinkedHashSet<>();  //创建储存还没有筛选的url的内容
        Set<String> deleteUrl = new LinkedHashSet<>();//储存将来要被删除的重复的url，因为有的url可能被重复超过2次，所以不能直接判断并且删除
        for(Element e : elementsAll)
        {
            String urlOriginal = e.attr("href");
            if (urlOriginal.contains("cp") == true) //判断这个网址是不是包含有cp
            {
                String[] urls = urlOriginal.split("#");//如果包含cp那么以#分割
                if(urlAll.add(urls[0]) == false)  //判断是否有已经存在的url，有的话全部删除
                {
                    deleteUrl.add(urls[0]);
                }
            }
        }
        for(String str : deleteUrl)  //清除那些不被需要的url
        {
            urlAll.remove(str);
        }

        //定义文件
        File file = new File("DangDangSpider.txt");
        PrintWriter output = new PrintWriter(file);

        //爬取所有的链接
        for(String url0 : urlAll)
        {
            String[] splitUrl = url0.split("/");
            String url1 = splitUrl[0] + "//" + splitUrl[2] + "/pg"; //整合url

            Set<String> judgeBookName = new LinkedHashSet<>(); //用来判断爬虫爬下来的书是否存在重复现象
            int page = 0;
            while (++page <= 100) {
                String url = url1 + page + "-" + splitUrl[3];
                System.out.println(url);
                Document document = Get(url);
                Element element = document.getElementById("component_0__0__6612");
                if (element == null)  //如果没有爬取到，那么就结束循环
                {
                    break;
                }
                Elements elements = element.getElementsByTag("li");

                //图片，书名，价格，评分，评论数，时间
                boolean judge = true;
                int count = 0;
                for (Element e : elements) {
                    //解析
                    String img; //图片
                    if (judge == true) {
                        img = e.select("img[src]").attr("src");
                        judge = false;
                    } else {
                        img = e.select("img[data-original]").attr("data-original");
                    }
                    String title = e.select("a[title]").attr("title"); //标题
                    String price = e.select("span[class]").select(".search_pre_price").text();  //价格

                    String score = e.select("span[style]").attr("style").split(" ")[1];  //评价
                    score = score.substring(0, score.length() - 1);

                    String comment = e.select("a[class]").select(".search_comment_num").text();  //评论数

                    String timeOriginal = e.select("p[class]").select(".search_book_author").text();  //出版日期
                    System.out.println(timeOriginal);
                    String time = "";
                    Date date = null;
                    String patterns = "./(\\d{4}-\\d{2}-\\d{2})";
                    Pattern pattern = Pattern.compile(patterns);
                    Matcher matcher = pattern.matcher(timeOriginal);
                    boolean isFind = matcher.find();
                    if (isFind == true)//判空
                    {
                        time = matcher.group(1);
                        date= Date.valueOf(time);
                    }

                    if (judgeBookName.add(title) == true)
                    { //判断书是否重复
                        //打印到控制台
                        System.out.print(img + "\t");
                        System.out.print(title + "\t");
                        System.out.print(price + "\t");
                        System.out.print(score + "\t");
                        System.out.print(comment + "\t");
                        System.out.println(time);
                        //写入文件
                        output.print(img + "\t");
                        output.print(title + "\t");
                        output.print(price + "\t");
                        output.print(score + "\t");
                        output.print(comment + "\t");
                        output.println(time);
                        System.out.println(++count);

                        //写入数据库
                        String sql="insert into book(img, title, price, score, comment, time)" +
                                "values(?,?,?,?,?,?)";//?作为占位符
                        PreparedStatement preparedStatement=connection.prepareStatement(sql);
                        preparedStatement.setString(1, img);//设置每个?参数值，第一个参数表示设置参数的位置，第二个表示参数的值
                        preparedStatement.setString(2, title);
                        preparedStatement.setString(3, price);
                        preparedStatement.setString(4, score);
                        preparedStatement.setString(5, comment);
                        preparedStatement.setDate(6, date);
                        preparedStatement.execute();
                    }
                }
            }
        }
        connection.close();
        output.close();
    }
}

