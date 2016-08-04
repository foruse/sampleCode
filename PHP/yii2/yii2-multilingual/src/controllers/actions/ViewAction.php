<?php
/**
 * Copyright © 2016 GBKSOFT. Web and Mobile Software Development.
 * See LICENSE.txt for license details.
 */
namespace gbksoft\multilingual\controllers\actions;

use gbksoft\multilingual\models\search\Language;
use yii\base\Action;

/**
 * Class ViewAction
 */
class ViewAction extends Action
{
    /**
     * @return string
     */
    public function run()
    {
        $searchModel = new Language();
        $dataProvider = $searchModel->search(\Yii::$app->request->getQueryParams());

        return $this->controller->render(
            'view',
            [
                'dataProvider' => $dataProvider,
                'searchModel' => $searchModel,
            ]
        );
    }
}
