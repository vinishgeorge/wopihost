package com.ethendev.wopihost;

import com.ethendev.wopihost.entity.FileInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.tomcat.util.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * WOPI HOST
 * Created by ethendev on 2017/4/15.
 */
@RestController
@RequestMapping(value="/wopi")
public class WopiHostController {

    Logger logger = LoggerFactory.getLogger(WopiHostController.class);

    @Value("${file.path}")
    private String filePath;

    private static final String CHARSET_UTF8 = "UTF-8";

    /**
     * 
Get file stream
     * @param name
     * @param response
     */
    @GetMapping("/files/{name}/contents")
    public void getFile(@PathVariable String name, HttpServletResponse response) {
        // 文件的路径
        String path = filePath + name;
        File file = new File(path);
        String filename = file.getName();

        try (InputStream fis = new BufferedInputStream(new FileInputStream(path));
             OutputStream toClient = new BufferedOutputStream(response.getOutputStream())) {
            byte[] buffer = new byte[fis.available()];
            fis.read(buffer);
            // 清空response
            response.reset();

            // 设置response的Header
            response.addHeader("Content-Disposition", "attachment;filename=" +
                    new String(filename.getBytes(CHARSET_UTF8), "ISO-8859-1"));
            response.addHeader("Content-Length", String.valueOf(file.length()));

            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            toClient.write(buffer);
            toClient.flush();
        } catch (IOException e) {
            logger.error("getFile failed, errMsg: {}", e.toString());
            e.printStackTrace();
        }
    }

    /**
     * 
		Saved update text
     * @param name
     * @param content
     */
    @PostMapping("/files/{name}/contents")
    public void postFile(@PathVariable(name = "name") String name, @RequestBody byte[] content) {
        // 文件的路径
        String path = filePath + name;
        File file = new File(path);

        try (FileOutputStream fop = new FileOutputStream(file)) {
            fop.write(content);
            fop.flush();
        } catch (IOException e) {
            logger.error("postFile failed, errMsg: {}", e.toString());
            e.printStackTrace();
        }
    }

    /**
     * 
Get file information

     * @param request
     * @param response
     * @return
     * @throws UnsupportedEncodingException
     */
    @GetMapping("/files/{name}")
    public void getFileInfo(HttpServletRequest request, HttpServletResponse response) {
        String uri = request.getRequestURI();
        FileInfo info = new FileInfo();
        info.setUserCanWrite(false);
        info.setSupportsUpdate(false);
        try  {
            // Get the file name to prevent garbled Chinese file names
            String fileName = URLDecoder.decode(uri.substring(uri.indexOf("wopi/files/") + 11), CHARSET_UTF8);
            if (fileName != null && fileName.length() > 0) {
                File file = new File(filePath + fileName);
                if (file.exists()) {
                    info.setBaseFileName(file.getName());
                    info.setSize(file.length());
                    info.setOwnerId("admin");
                    info.setVersion(file.lastModified());
                    info.setSha256(getHash256(file));
                }
            }

            ObjectMapper mapper = new ObjectMapper();
            response.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
            response.getWriter().write(mapper.writeValueAsString(info));
        } catch (Exception e) {
            logger.error("getFileInfo failed, errMsg: {}", e.toString());
            e.printStackTrace();
        }
    }

    /**
     *
Get the SHA-256 value of the file
     * @param file
     * @return
     */
    private String getHash256(File file) throws IOException, NoSuchAlgorithmException {
        String value = "";
        try (InputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[1024];
            int numRead;
            // Returns a MessageDigest object that implements the specified digest algorithm
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            do {
                numRead = fis.read(buffer);
                if (numRead > 0) {
                    // Update summary
                    digest.update(buffer, 0, numRead);
                }
            } while (numRead != -1);

            value = new String(Base64.encodeBase64(digest.digest()));
        }
        return value;
    }

}
