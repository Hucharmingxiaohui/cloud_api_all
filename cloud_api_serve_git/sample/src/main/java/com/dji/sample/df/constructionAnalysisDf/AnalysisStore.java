package com.dji.sample.df.constructionAnalysisDf;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/** All access is confined to configured roots; symlinks are deliberately unsupported. */
public final class AnalysisStore {
    private final Path input, output;
    final ObjectMapper json = new ObjectMapper();
    public AnalysisStore(Path input, Path output) {
        this.input=input.toAbsolutePath().normalize(); this.output=output.toAbsolutePath().normalize();
        if(this.input.startsWith(this.output)||this.output.startsWith(this.input))
            throw new IllegalArgumentException("Input and output roots must be separate");
    }
    public static String id(String value) {
        if(value==null||!value.matches("[A-Za-z0-9][A-Za-z0-9_-]{0,127}")
                ||value.matches("(?i)(CON|PRN|AUX|NUL|COM[0-9]|LPT[0-9])"))
            throw error(HttpStatus.BAD_REQUEST,"Invalid path identifier");
        return value;
    }
    private Path safe(Path path) throws IOException {
        Path absolute=path.toAbsolutePath().normalize();
        for(Path p=absolute;p!=null;p=p.getParent()) {
            if(Files.isSymbolicLink(p)) throw error(HttpStatus.BAD_REQUEST,"Symbolic links are not allowed");
            if(Files.exists(p,LinkOption.NOFOLLOW_LINKS)&&!p.toRealPath().equals(p.toAbsolutePath()))
                throw error(HttpStatus.BAD_REQUEST,"Storage path resolves outside its namespace");
        }
        return absolute;
    }
    public Map<String,Path> scan(String task) throws IOException {
        Path dir=safe(input.resolve(id(task)));
        if(!Files.isDirectory(dir,LinkOption.NOFOLLOW_LINKS))throw error(HttpStatus.NOT_FOUND,"Input task folder not found");
        Map<String,Path> files=new TreeMap<>();
        try(DirectoryStream<Path> stream=Files.newDirectoryStream(dir)) {
            for(Path p:stream) {
                if(Files.isSymbolicLink(p))throw error(HttpStatus.BAD_REQUEST,"Symbolic links are not allowed in task folder");
                if(!Files.isRegularFile(p,LinkOption.NOFOLLOW_LINKS))continue;
                String name=p.getFileName().toString();int dot=name.lastIndexOf('.');
                if(dot<=0||!Set.of("jpg","jpeg","png","tif","tiff","dng","bmp","webp","heic","heif").contains(name.substring(dot+1).toLowerCase(Locale.ROOT)))continue;
                String image=id(name.substring(0,dot));
                if(files.put(image,safe(p))!=null)throw error(HttpStatus.CONFLICT,"Duplicate image identifiers in input folder");
            }
        }
        return files;
    }
    public Path task(String workspace,String task) throws IOException {return safe(output.resolve(id(workspace)).resolve(id(task)));}
    public void claim(String workspace,String task) throws IOException {
        id(workspace);id(task);Path owners=safe(output.resolve(".owners"));Files.createDirectories(owners);
        Path lock=safe(owners.resolve(task+".lock"));
        try(FileChannel channel=FileChannel.open(lock,StandardOpenOption.CREATE,StandardOpenOption.WRITE)) {
            try(java.nio.channels.FileLock held=channel.tryLock()) {
                if(held==null)throw error(HttpStatus.CONFLICT,"Task ownership is being assigned");
                Path owner=file(owners,task+".json");Map<String,Object> saved=read(owner);
                if(saved.isEmpty())write(owner,Map.of("workspaceId",workspace,"taskId",task));
                else if(!workspace.equals(saved.get("workspaceId")))throw error(HttpStatus.NOT_FOUND,"Task is assigned to another workspace");
            }catch(java.nio.channels.OverlappingFileLockException e){throw error(HttpStatus.CONFLICT,"Task ownership is being assigned");}
        }
    }
    public Path batch(String workspace,String task,String batch) throws IOException {return safe(task(workspace,task).resolve(id(batch)));}
    public Path file(Path parent,String name) throws IOException {
        if(name.contains("/")||name.contains("\\")||name.equals(".."))throw error(HttpStatus.BAD_REQUEST,"Invalid storage file");
        return safe(parent.resolve(name));
    }
    public Map<String,Object> read(Path path) throws IOException {
        safe(path);if(!Files.exists(path,LinkOption.NOFOLLOW_LINKS))return new LinkedHashMap<>();
        if(Files.size(path)>32L*1024*1024)throw error(HttpStatus.INTERNAL_SERVER_ERROR,"Journal exceeds limit");
        return json.readValue(path.toFile(),new TypeReference<LinkedHashMap<String,Object>>(){});
    }
    public void write(Path path,Map<String,Object> value) throws IOException {writeBytes(path,json.writeValueAsBytes(value));}
    public void writeBytes(Path path,byte[] bytes) throws IOException {
        safe(path);Files.createDirectories(safe(path.getParent()));safe(path.getParent());
        Path temp=Files.createTempFile(path.getParent(),"journal-",".tmp");
        try {
            Files.write(temp,bytes);
            try(FileChannel ch=FileChannel.open(temp,StandardOpenOption.WRITE)){ch.force(true);}
            safe(path);Files.move(temp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
        } finally {Files.deleteIfExists(temp);}
    }
    public List<Map<String,Object>> batches(String workspace,String task) throws IOException {
        Path parent=task(workspace,task);List<Map<String,Object>> out=new ArrayList<>();
        if(!Files.exists(parent))return out;
        try(DirectoryStream<Path> dirs=Files.newDirectoryStream(parent)) {
            for(Path dir:dirs) {
                safe(dir);if(!Files.isDirectory(dir,LinkOption.NOFOLLOW_LINKS))continue;
                Map<String,Object> b=read(file(dir,"batch.json"));if(!b.isEmpty())out.add(b);
            }
        }
        return out.stream().sorted(Comparator.comparing(b->String.valueOf(b.get("createdAt")),Comparator.reverseOrder())).collect(Collectors.toList());
    }
    public byte[] bytes(Path path,long limit) throws IOException {
        safe(path);if(!Files.isRegularFile(path,LinkOption.NOFOLLOW_LINKS))throw error(HttpStatus.NOT_FOUND,"Evidence not found");
        try(InputStream in=Files.newInputStream(path);ByteArrayOutputStream out=new ByteArrayOutputStream()) {
            byte[] buffer=new byte[8192];int n;long count=0;
            while((n=in.read(buffer))!=-1){count+=n;if(count>limit)throw error(HttpStatus.PAYLOAD_TOO_LARGE,"Image exceeds configured limit");out.write(buffer,0,n);}
            return out.toByteArray();
        }
    }
    public static ResponseStatusException error(HttpStatus status,String message) {return new ResponseStatusException(status,message);}
}
