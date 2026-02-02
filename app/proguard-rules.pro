# Shizuku reflection keep rules
-keep class rikka.shizuku.Shizuku {
    public static java.lang.Process newProcess(java.lang.String[], java.lang.String[], java.lang.String);
    public static boolean isLimited();
}
