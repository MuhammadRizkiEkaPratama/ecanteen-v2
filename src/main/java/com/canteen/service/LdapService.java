package com.canteen.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.naming.Context;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.StartTlsRequest;
import javax.naming.ldap.StartTlsResponse;
import java.util.Hashtable;

/**
 * Asks the LDAP directory whether a username + password pair is correct.
 * One job only: returns true or false. It does not read roles or stalls —
 * those stay in the users table.
 */
@Service
public class LdapService {

    private final String url;
    private final String userPattern;

    public LdapService(
            @Value("${ecanteen.ldap.url:}") String url,
            @Value("${ecanteen.ldap.user-pattern:uid=%s,ou=people,dc=bsp,dc=id}") String userPattern) {
        this.url = url;
        this.userPattern = userPattern;
    }

    /** LDAP is only used when a URL is configured. */
    public boolean aktif() {
        return url != null && !url.isBlank();
    }

    public boolean cekSandi(String username, String password) {
        // An EMPTY password must never reach LDAP. In LDAP, a bind with a
        // valid user but empty password is an "unauthenticated bind" and many
        // servers answer SUCCESS. Without this check, anyone who leaves the
        // password box empty would be logged in.
        if (!aktif() || username == null || username.isBlank()
                || password == null || password.isEmpty()) {
            return false;
        }

        // The password is NOT put in this table. See the comment below.
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, url);
        // Without timeouts, a dead LDAP server makes the login page hang until
        // the operating system gives up, which can take minutes.
        env.put("com.sun.jndi.ldap.connect.timeout", "3000");
        env.put("com.sun.jndi.ldap.read.timeout", "5000");

        LdapContext ctx = null;
        StartTlsResponse tls = null;
        try {
            // ---------------------------------------------------------------
            // STARTTLS DULU, SANDI BELAKANGAN. URUTANNYA YANG PENTING.
            //
            // Direktori LDAP ada di lokasi lain, jadi tiap login mengirim
            // sandi MELEWATI ROUTER PENYEDIA JASA. Sebelum 13 Agustus 2026
            // sambungan ini polos: siapa pun yang bisa membaca lalu lintas di
            // jalur itu bisa membaca sandi setiap orang.
            //
            // StartTLS menaikkan sambungan biasa menjadi terenkripsi. Yang
            // menentukan aman-tidaknya bukan sekadar memakainya, melainkan
            // URUTANNYA: kalau username dan password dimasukkan ke `env` di
            // atas, JNDI mengirimkannya SAAT sambungan dibuat — sebelum TLS
            // sempat dinegosiasikan. Enkripsinya jadi datang terlambat.
            //
            // Jadi: sambung dulu tanpa kredensial, naikkan ke TLS, BARU
            // pasang kredensialnya dan minta bind lewat `reconnect`.
            // ---------------------------------------------------------------
            ctx = new InitialLdapContext(env, null);

            tls = (StartTlsResponse) ctx.extendedOperation(new StartTlsRequest());
            // `negotiate()` memverifikasi sertifikat server terhadap
            // truststore Java. Sertifikatnya diterbitkan CA internal lab, dan
            // CA itu sudah dipasang ke penyimpanan kepercayaan sistem di
            // server ini. Kalau verifikasinya gagal, baris ini melempar dan
            // sandi TIDAK PERNAH dikirim.
            tls.negotiate();

            // Baru sekarang kredensialnya dipasang, di atas sambungan yang
            // sudah terenkripsi.
            ctx.addToEnvironment(Context.SECURITY_AUTHENTICATION, "simple");
            ctx.addToEnvironment(Context.SECURITY_PRINCIPAL,
                    String.format(userPattern, username));
            ctx.addToEnvironment(Context.SECURITY_CREDENTIALS, password);
            ctx.reconnect(null);   // inilah bind-nya, dan ia lewat TLS

            return true;
        } catch (Exception e) {
            // Gagal TLS dan sandi salah sama-sama berakhir di sini, dan
            // keduanya memang harus berakhir sebagai "tidak boleh masuk".
            // Yang TIDAK boleh terjadi: mencoba lagi tanpa enkripsi kalau TLS
            // gagal. Itu akan membuat gangguan sertifikat berubah diam-diam
            // menjadi kebocoran sandi.
            return false;
        } finally {
            try {
                if (tls != null) tls.close();
            } catch (Exception abaikan) {
                // Menutup lapisan TLS yang sudah putus bukan kegagalan login.
            }
            try {
                if (ctx != null) ctx.close();
            } catch (Exception abaikan) {
                // Sama: sambungan yang sudah tertutup tidak perlu dilaporkan.
            }
        }
    }
}
