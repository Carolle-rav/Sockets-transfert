package transfert;

public class Slave {
    private String ip;
    private int port;
    private String directory;

    public Slave(String ip, int port, String directory) {
        this.ip = ip;
        this.port = port;
        this.directory = directory;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    @Override
    public String toString() {
        return "Slave{" +
                "ip='" + ip + '\'' +
                ", port=" + port +
                ", directory='" + directory + '\'' +
                '}';
    }
}
