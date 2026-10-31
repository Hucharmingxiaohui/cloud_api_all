package com.dji.sample.df.constructionAnalysisDf;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import java.io.*;
import java.net.*;
import java.util.*;
import static com.dji.sample.df.constructionAnalysisDf.AnalysisStore.error;

public class AnalysisClient {
    final RestTemplate http;
    private final String base,token;
    private final long maxResponse;
    private final ObjectMapper json=new ObjectMapper();
    public AnalysisClient(String base,String token,int connect,int read,long maxResponse) {
        this.base=base==null?"":base.replaceAll("/+$","");this.token=token;this.maxResponse=maxResponse;
        if(!this.base.isBlank()) {
            URI uri=URI.create(this.base);
            if(!Set.of("http","https").contains(uri.getScheme())||uri.getHost()==null||uri.getUserInfo()!=null||uri.getQuery()!=null||uri.getFragment()!=null)
                throw new IllegalArgumentException("Invalid algorithm base URL");
        }
        SimpleClientHttpRequestFactory factory=new SimpleClientHttpRequestFactory(){
            @Override protected void prepareConnection(HttpURLConnection c,String method)throws IOException {
                super.prepareConnection(c,method);c.setInstanceFollowRedirects(false);
            }
        };
        factory.setConnectTimeout(connect);factory.setReadTimeout(read);http=new RestTemplate(factory);
        // Do not buffer remote error bodies or include remote addresses in API errors.
        http.setErrorHandler(new org.springframework.web.client.ResponseErrorHandler(){
            public boolean hasError(org.springframework.http.client.ClientHttpResponse r){return false;}
            public void handleError(org.springframework.http.client.ClientHttpResponse r){}
        });
    }
    public boolean configured(){return !base.isBlank();}
    public Map<String,Object> health(){return call(HttpMethod.GET,"/health",null,200);}
    public Map<String,Object> submit(Map<String,Object> request){return call(HttpMethod.POST,"/analysis/tasks",request,202);}
    public Map<String,Object> poll(String remote){return call(HttpMethod.GET,"/analysis/tasks/"+AnalysisStore.id(remote),null,200);}
    public Map<String,Object> image(String remote,String image){return call(HttpMethod.GET,"/analysis/tasks/"+AnalysisStore.id(remote)+"/images/"+AnalysisStore.id(image),null,200);}
    private Map<String,Object> call(HttpMethod method,String path,Map<String,Object> body,int expected) {
        if(!configured())throw error(HttpStatus.SERVICE_UNAVAILABLE,"Algorithm service is not configured");
        try {
            return http.execute(URI.create(base+path),method,request->{
                request.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                if(token!=null&&!token.isBlank())request.getHeaders().setBearerAuth(token);
                if(body!=null)json.writeValue(request.getBody(),body);
            },response->{
                if(response.getRawStatusCode()!=expected) {
                    HttpStatus status=HttpStatus.resolve(response.getRawStatusCode());
                    if(status==HttpStatus.PAYLOAD_TOO_LARGE)throw error(status,"Algorithm rejected oversized request");
                    throw error(HttpStatus.BAD_GATEWAY,"Algorithm HTTP response "+response.getRawStatusCode());
                }
                ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[8192];int n;long count=0;
                while((n=response.getBody().read(buf))!=-1){count+=n;if(count>maxResponse)throw error(HttpStatus.BAD_GATEWAY,"Algorithm response exceeds limit");out.write(buf,0,n);}
                return json.readValue(out.toByteArray(),new TypeReference<LinkedHashMap<String,Object>>(){});
            });
        }catch(org.springframework.web.server.ResponseStatusException e){throw e;}
        catch(Exception e){throw error(HttpStatus.BAD_GATEWAY,"Algorithm connection or response failed");}
    }
}
