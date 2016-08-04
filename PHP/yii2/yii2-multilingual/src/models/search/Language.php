<?php
/**
 * Copyright © 2016 GBKSOFT. Web and Mobile Software Development.
 * See LICENSE.txt for license details.
 */
namespace gbksoft\multilingual\models\search;

use gbksoft\multilingual\models\Language as MainLanguage;
use yii\data\ActiveDataProvider;

/**
 * Class Language
 */
class Language extends MainLanguage
{
    /**
     * @inheritdoc
     */
    public function rules()
    {
        return [
            ['id', 'integer'],
            [['url', 'locale', 'name'], 'safe'],
        ];
    }

    /**
     * Searching menu
     *
     * @param  array $params
     * @return ActiveDataProvider
     */
    public function search($params)
    {
        $query = self::find()->from(self::tableName());

        $dataProvider = new ActiveDataProvider(['query' => $query]);

        $sort = $dataProvider->getSort();

        $sort->attributes['name'] = [
            'asc' => ['name' => SORT_ASC],
            'desc' => ['name' => SORT_DESC],
            'label' => 'name',
        ];

        $sort->attributes['locale'] = [
            'asc' => ['locale' => SORT_ASC],
            'desc' => ['locale' => SORT_DESC],
            'label' => 'locale',
        ];

        $sort->attributes['url'] = [
            'asc' => ['url' => SORT_ASC],
            'desc' => ['url' => SORT_DESC],
            'label' => 'url',
        ];

        $sort->defaultOrder = ['name' => SORT_ASC];

        if (!($this->load($params) && $this->validate())) {
            return $dataProvider;
        }

        $query->andFilterWhere(['like', 'lower(name)', strtolower($this->name)])
            ->andFilterWhere(['like', 'lower(locale)', strtolower($this->locale)])
            ->andFilterWhere(['like', 'lower(url)', strtolower($this->url)]);

        return $dataProvider;
    }
}
