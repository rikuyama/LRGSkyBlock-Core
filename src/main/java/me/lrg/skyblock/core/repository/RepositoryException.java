package me.lrg.skyblock.core.repository;

/**
 * Repository層で発生したエラーを表す例外クラス。
 *
 * SQLExceptionをそのまま上の層へ投げず、
 * Repositoryで起きたエラーとして扱うために使う。
 */
public class RepositoryException extends RuntimeException {

    public RepositoryException(String message, Throwable cause) {
        super(message, cause);
    }
}