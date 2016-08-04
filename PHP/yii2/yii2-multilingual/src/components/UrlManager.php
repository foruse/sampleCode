<?php
/**
 * Copyright © 2016 GBKSOFT. Web and Mobile Software Development.
 * See LICENSE.txt for license details.
 */
namespace gbksoft\multilingual\components;

use gbksoft\multilingual\models\Language;
use yii\web\UrlManager as BaseUrlManager;

/**
 * Class UrlManager
 */
class UrlManager extends BaseUrlManager
{
    const DEFAULT_KEY = '<default>';

    const SUFFIX_PROTOCOL = '://';

    const SUFFIX_PROTOCOL_PLACEHOLDER = '<pefix-protocol>';

    /**
     * @var Language[]
     */
    private $languages;

    /**
     * @var LanguageQueryFactory
     */
    private $queryFactory;

    /**
     * Constructor
     *
     * @param LanguageQueryFactory $queryFactory
     * @param array $config
     */
    public function __construct(LanguageQueryFactory $queryFactory, $config = [])
    {
        parent::__construct($config);

        $this->queryFactory = $queryFactory;
    }

    /**
     * @inheritdoc
     */
    public function createUrl($params)
    {
        $params = (array) $params;
        $language = $this->getLanguage(isset($params['language']) ? $params['language'] : self::DEFAULT_KEY);
        unset($params['language']);

        return '/' . $language->url . '/' . ltrim(parent::createUrl($params), '/');
    }

    /**
     * @param string $url
     * @return Language
     */
    private function getLanguage($url)
    {
        if (isset($this->languages[$url])) {
            return $this->languages[$url];
        }

        $query = $this->queryFactory->create();

        $language = $query->getByUrl($url)->one();

        if ($language === null) {
            $language = $query->getDefault()->one();
            $url = self::DEFAULT_KEY;
        }

        $this->languages[$url] = $language;

        return $this->languages[$url];
    }
}
