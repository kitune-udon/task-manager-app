package com.example.task.security;

import com.example.task.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * アプリケーションの User エンティティを Spring Security 用の認証情報に変換したもの。
 */
public class CustomUserDetails implements UserDetails {

    private final Long id;
    private final String name;
    private final String email;
    private final String password;

    /**
     * Spring Security が扱うユーザー詳細情報を生成する。
     *
     * @param id ユーザーID
     * @param name 表示名
     * @param email メールアドレス
     * @param password ハッシュ化済みパスワード
     */
    public CustomUserDetails(Long id, String name, String email, String password) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
    }

    /**
     * 永続化済みユーザーを認証コンテキストへ載せられる形式に変換する。
     *
     * @param user ユーザーエンティティ
     * @return Spring Security 用のユーザー詳細情報
     */
    public static CustomUserDetails from(User user) {
        return new CustomUserDetails(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPassword()
        );
    }

    /**
     * 認証済みユーザーのIDを取得する。
     *
     * @return ユーザーID
     */
    public Long getId() {
        return id;
    }

    /**
     * 画面表示などに利用するユーザー名を取得する。
     *
     * @return 表示名
     */
    public String getDisplayName() {
        return name;
    }

    /**
     * ユーザーに付与する権限を取得する。
     *
     * <p>現時点では全ユーザーを一般ユーザーとして扱う。</p>
     *
     * @return ユーザー権限
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    /**
     * 認証に使用するハッシュ化済みパスワードを取得する。
     *
     * @return ハッシュ化済みパスワード
     */
    @Override
    public String getPassword() {
        return password;
    }

    /**
     * Spring Security 上のユーザー名を取得する。
     *
     * <p>このアプリケーションではメールアドレスをログイン識別子として使用する。</p>
     *
     * @return メールアドレス
     */
    @Override
    public String getUsername() {
        return email;
    }

    /**
     * アカウントの有効期限状態を返す。
     *
     * @return アカウントを期限切れ扱いしないため常にtrue
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * アカウントのロック状態を返す。
     *
     * @return アカウントをロック扱いしないため常にtrue
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * 認証情報の有効期限状態を返す。
     *
     * @return 認証情報を期限切れ扱いしないため常にtrue
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * アカウントの有効状態を返す。
     *
     * @return アカウントを無効扱いしないため常にtrue
     */
    @Override
    public boolean isEnabled() {
        return true;
    }
}
