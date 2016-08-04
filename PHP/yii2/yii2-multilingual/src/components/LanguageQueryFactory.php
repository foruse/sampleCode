<?php
/**
 * Copyright © 2016 GBKSOFT. Web and Mobile Software Development.
 * See LICENSE.txt for license details.
 */
namespace gbksoft\multilingual\components;

use gbksoft\multilingual\models\Language;
use gbksoft\multilingual\models\queries\LanguageQuery;

/**
 * Class LanguageQueryFactory
 */
class LanguageQueryFactory
{
    /**
     * @return LanguageQuery
     */
    public function create()
    {
        return \Yii::createObject(LanguageQuery::class, [Language::class]);
    }
}
