package com.limiter.open;

import com.limiter.open.config.ConfigLoad;
import com.limiter.open.config.ConfigLoadImpl;
import com.limiter.open.config.RuleDao;
import com.limiter.open.config.RuleDaoImpl;
import com.limiter.open.config.parse.LocalFileParseServiceImpl;
import com.limiter.open.config.parse.ParseService;
import com.limiter.open.lock.LockService;
import com.limiter.open.lock.impl.LocalLockServiceImpl;
import com.limiter.open.service.OpenPlatformService;
import com.limiter.open.service.impl.ConfigCallBackImpl;
import com.limiter.open.service.impl.OpenPlatformServiceImpl;
import com.limiter.open.tokenbucket.config.ConfigCallBack;
import com.limiter.open.tokenbucket.config.ConfigCenter;
import com.limiter.open.tokenbucket.config.impl.LocalConfigCenterImpl;
import com.limiter.open.tokenbucket.core.TokenBucketDao;
import com.limiter.open.tokenbucket.core.TokenBucketManager;
import com.limiter.open.tokenbucket.core.TokenBucketService;
import com.limiter.open.tokenbucket.core.TokenFilledStrategy;
import com.limiter.open.tokenbucket.core.impl.DefaultTokenFilledStrategy;
import com.limiter.open.tokenbucket.core.impl.LocalTokenBucketDaoImpl;
import com.limiter.open.tokenbucket.core.impl.TimeToolsImpl;
import com.limiter.open.tokenbucket.core.impl.TokenBucketServiceImpl;

/**
 * @author wuhao
 */
public class LimiterFacade implements OpenPlatformService {

    private ParseService parseService;

    private RuleDao ruleDao;

    private ConfigCenter configCenter;

    private ConfigLoad configLoad;

    private LockService lockService;

    private TokenBucketDao tokenBucketDAO;

    private TokenFilledStrategy tokenFilledStrategy;

    private OpenPlatformService openPlatformServiceProxy;

    private String configPath;

    private volatile boolean isLoad;

    public void setParseService(ParseService parseService) {
        this.parseService = parseService;
    }

    public void setRuleDao(RuleDao ruleDao) {
        this.ruleDao = ruleDao;
    }

    public void setConfigCenter(ConfigCenter configCenter) {
        this.configCenter = configCenter;
    }

    public void setConfigLoad(ConfigLoad configLoad) {
        this.configLoad = configLoad;
    }

    public void setLockService(LockService lockService) {
        this.lockService = lockService;
    }

    public void setTokenBucketDAO(TokenBucketDao tokenBucketDAO) {
        this.tokenBucketDAO = tokenBucketDAO;
    }

    public void setTokenFilledStrategy(TokenFilledStrategy tokenFilledStrategy) {
        this.tokenFilledStrategy = tokenFilledStrategy;
    }

    @Override
    public boolean visit(String appkey, String method) {
        init();
        return openPlatformServiceProxy.visit(appkey, method);
    }

    private void init() {
        if (isLoad) {
            return;
        }
        synchronized (this) {
            if (!isLoad) {
                load();
                isLoad = true;
            }
        }
    }

    private void load() {
        if (null == parseService) {
            parseService = new LocalFileParseServiceImpl();
            ((LocalFileParseServiceImpl) parseService).setConfigPath(configPath);
        }

        if (null == configCenter) {
            configCenter = new LocalConfigCenterImpl();
        }

        if (null == lockService) {
            lockService = new LocalLockServiceImpl();
        }

        if (null == tokenBucketDAO) {
            tokenBucketDAO = new LocalTokenBucketDaoImpl();
        }

        if (null == ruleDao) {
            ruleDao = new RuleDaoImpl();
            ((RuleDaoImpl) ruleDao).setParseService(parseService);
        }

        if (null == tokenFilledStrategy) {
            tokenFilledStrategy = new DefaultTokenFilledStrategy();
            ((DefaultTokenFilledStrategy) tokenFilledStrategy).setTimeTools(new TimeToolsImpl());
        }

        if (null == configLoad) {
            configLoad = new ConfigLoadImpl();
            ((ConfigLoadImpl) configLoad).setConfigCenter(configCenter);
            ((ConfigLoadImpl) configLoad).setRuleDao(ruleDao);
        }

        TokenBucketService tokenBucketService = new TokenBucketServiceImpl();
        TokenBucketManager tokenBucketManager = new TokenBucketManager();
        tokenBucketManager.setLockService(lockService);
        tokenBucketManager.setTokenBucketDAO(tokenBucketDAO);
        ((TokenBucketServiceImpl) tokenBucketService).setTokenBucketManager(tokenBucketManager);
        ((TokenBucketServiceImpl) tokenBucketService).setTokenFilledStrategy(tokenFilledStrategy);
        ((TokenBucketServiceImpl) tokenBucketService).setConfigCenter(configCenter);
        ((TokenBucketServiceImpl) tokenBucketService).setTimeTools(new TimeToolsImpl());

        ConfigCallBack configCallBack = new ConfigCallBackImpl();
        ((TokenBucketServiceImpl) tokenBucketService).setConfigCallBack(configCallBack);

        openPlatformServiceProxy = new OpenPlatformServiceImpl();
        ((OpenPlatformServiceImpl) openPlatformServiceProxy).setTokenBucketService(tokenBucketService);
        configLoad.load();
    }
}
